package com.example.chat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.chat.model.ChatMessage;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@Testcontainers
public class LoadTest {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management")
            .withExposedPorts(5672, 15672);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Test
    public void testHighVolumeMessageProcessing() throws InterruptedException {
        // 테스트 파라미터
        int numberOfThreads = 10;  // 동시 발송 스레드 수
        int messagesPerThread = 1000;  // 각 스레드당 메시지 수
        int totalMessages = numberOfThreads * messagesPerThread;
        
        // 성능 측정용
        AtomicInteger sentCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> latencies = new CopyOnWriteArrayList<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);

        long startTime = System.currentTimeMillis();

        // 여러 스레드로 메시지 발송
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작
                    
                    for (int j = 0; j < messagesPerThread; j++) {
                        long messageStart = System.currentTimeMillis();
                        try {
                            ChatMessage message = ChatMessage.builder()
                                    .roomId("room-" + (threadId % 5)) // 5개 채팅방 분산
                                    .sender("user-" + threadId)
                                    .senderId("user-" + threadId)
                                    .content("Load test message " + j + " from thread " + threadId)
                                    .timestamp(LocalDateTime.now())
                                    .type(ChatMessage.MessageType.CHAT)
                                    .build();
                            
                            rabbitTemplate.convertAndSend("chat.exchange", "chat.room." + message.getRoomId(), message);
                            sentCount.incrementAndGet();
                            
                            long messageEnd = System.currentTimeMillis();
                            latencies.add(messageEnd - messageStart);
                            
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // 모든 스레드 동시 시작
        startLatch.countDown();
        
        // 완료 대기
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // 결과 분석
        System.out.println("\n=== 부하테스트 결과 ===");
        System.out.println("총 메시지 수: " + totalMessages);
        System.out.println("성공적으로 전송된 메시지: " + sentCount.get());
        System.out.println("에러 발생: " + errorCount.get());
        System.out.println("총 소요 시간: " + totalTime + "ms");
        System.out.println("평균 처리량: " + (sentCount.get() * 1000.0 / totalTime) + " messages/sec");
        
        if (!latencies.isEmpty()) {
            double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
            long maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
            long minLatency = latencies.stream().mapToLong(Long::longValue).min().orElse(0);
            
            System.out.println("평균 레이턴시: " + avgLatency + "ms");
            System.out.println("최대 레이턴시: " + maxLatency + "ms");
            System.out.println("최소 레이턴시: " + minLatency + "ms");
        }

        // 검증
        assertTrue(completed, "모든 메시지가 60초 내에 처리되어야 함");
        assertEquals(totalMessages, sentCount.get(), "모든 메시지가 성공적으로 전송되어야 함");
        assertEquals(0, errorCount.get(), "에러가 발생하지 않아야 함");
    }

    @Test
    public void testBurstLoad() throws InterruptedException {
        // 순간적인 대량 메시지 처리 테스트
        int burstSize = 5000;
        AtomicInteger processedCount = new AtomicInteger(0);
        
        // 메시지 리스너 설정 (실제 처리 시뮬레이션)
        rabbitTemplate.setReceiveTimeout(100);
        
        long startTime = System.currentTimeMillis();
        
        // 한번에 대량 메시지 전송
        for (int i = 0; i < burstSize; i++) {
            ChatMessage message = ChatMessage.builder()
                    .roomId("burst-room")
                    .sender("burst-user")
                    .senderId("burst-user")
                    .content("Burst message " + i)
                    .timestamp(LocalDateTime.now())
                    .type(ChatMessage.MessageType.CHAT)
                    .build();
                    
            rabbitTemplate.convertAndSend("chat.exchange", "chat.room.burst-room", message);
        }
        
        long endTime = System.currentTimeMillis();
        long sendTime = endTime - startTime;
        
        System.out.println("\n=== 순간 부하 테스트 결과 ===");
        System.out.println("총 메시지 수: " + burstSize);
        System.out.println("전송 소요 시간: " + sendTime + "ms");
        System.out.println("전송 속도: " + (burstSize * 1000.0 / sendTime) + " messages/sec");
        
        assertTrue(sendTime < 10000, "5000개 메시지가 10초 내에 전송되어야 함");
    }
}
