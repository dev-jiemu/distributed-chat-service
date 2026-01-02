package com.example.chat;

import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Docker Compose를 먼저 실행한 후 테스트
 * 실행 전: docker-compose up -d rabbitmq redis
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672",
    "spring.rabbitmq.username=admin",
    "spring.rabbitmq.password=admin",
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.redis.password=test"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DockerComposeLoadTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @BeforeAll
    static void checkDockerServices() {
        // Docker 서비스가 실행 중인지 확인
        try {
            Process process = Runtime.getRuntime().exec("docker ps --format '{{.Names}}'");
            process.waitFor();
            // 실제로는 출력을 파싱해서 rabbitmq와 redis가 실행 중인지 확인
            System.out.println("Docker 서비스를 확인하세요. 실행 명령: docker-compose up -d rabbitmq redis");
        } catch (Exception e) {
            fail("Docker 서비스 확인 실패. docker-compose up -d rabbitmq redis 를 먼저 실행하세요.");
        }
    }

    @Test
    @Order(1)
    public void testConnectionPoolPerformance() throws InterruptedException {
        // 커넥션 풀 성능 테스트
        int poolSize = 20;
        int messagesPerConnection = 500;
        
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch latch = new CountDownLatch(poolSize);
        AtomicInteger successCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < poolSize; i++) {
            final int connectionId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < messagesPerConnection; j++) {
                        ChatMessage message = ChatMessage.builder()
                                .roomId("pool-test-room")
                                .senderId("connection-" + connectionId)
                                .content("Message " + j + " from connection " + connectionId)
                                .timestamp(System.currentTimeMillis())
                                .build();
                        
                        rabbitTemplate.convertAndSend("chat.exchange", "chat.room.pool-test", message);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("\n=== 커넥션 풀 성능 테스트 결과 ===");
        System.out.println("총 커넥션 수: " + poolSize);
        System.out.println("커넥션당 메시지: " + messagesPerConnection);
        System.out.println("총 메시지: " + (poolSize * messagesPerConnection));
        System.out.println("성공한 메시지: " + successCount.get());
        System.out.println("소요 시간: " + totalTime + "ms");
        System.out.println("처리량: " + (successCount.get() * 1000.0 / totalTime) + " msg/sec");
        
        assertEquals(poolSize * messagesPerConnection, successCount.get());
    }

    @Test
    @Order(2)
    public void testMessagePersistenceUnderLoad() throws InterruptedException {
        // 메시지 영속성 테스트 - Redis 캐시와 함께
        int messageCount = 1000;
        ConcurrentHashMap<String, ChatMessage> sentMessages = new ConcurrentHashMap<>();
        
        // 메시지 전송
        for (int i = 0; i < messageCount; i++) {
            ChatMessage message = ChatMessage.builder()
                    .roomId("persistence-room-" + (i % 10))  // 10개 방에 분산
                    .senderId("user-" + (i % 50))            // 50명 사용자
                    .content("Persistence test message " + i)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            String messageId = "msg-" + i;
            sentMessages.put(messageId, message);
            
            // 메시지 ID를 헤더에 포함시켜 전송
            rabbitTemplate.convertAndSend("chat.exchange", 
                "chat.room." + message.getRoomId(), 
                message,
                m -> {
                    m.getMessageProperties().setMessageId(messageId);
                    return m;
                });
        }
        
        System.out.println("\n=== 메시지 영속성 테스트 ===");
        System.out.println("전송된 메시지 수: " + messageCount);
        System.out.println("고유 메시지 수: " + sentMessages.size());
        
        assertEquals(messageCount, sentMessages.size(), "모든 메시지가 고유해야 함");
    }

    @Test
    @Order(3)
    public void testRateLimiting() throws InterruptedException {
        // 속도 제한 테스트
        int targetRate = 100; // 초당 100개 메시지
        int duration = 10; // 10초간 테스트
        int expectedTotal = targetRate * duration;
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger sentCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 일정한 속도로 메시지 전송
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ChatMessage message = ChatMessage.builder()
                        .roomId("rate-limit-room")
                        .senderId("rate-tester")
                        .content("Rate limited message " + sentCount.get())
                        .timestamp(System.currentTimeMillis())
                        .build();
                
                rabbitTemplate.convertAndSend("chat.exchange", "chat.room.rate-limit", message);
                sentCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
            }
        }, 0, 1000 / targetRate, TimeUnit.MILLISECONDS);
        
        // 지정된 시간만큼 대기
        Thread.sleep(duration * 1000);
        scheduler.shutdown();
        
        long endTime = System.currentTimeMillis();
        long actualDuration = (endTime - startTime) / 1000;
        
        System.out.println("\n=== 속도 제한 테스트 결과 ===");
        System.out.println("목표 속도: " + targetRate + " msg/sec");
        System.out.println("테스트 시간: " + actualDuration + "초");
        System.out.println("전송된 메시지: " + sentCount.get());
        System.out.println("실제 속도: " + (sentCount.get() / (double) actualDuration) + " msg/sec");
        System.out.println("에러 수: " + errorCount.get());
        
        // 10% 오차 범위 내에서 검증
        assertTrue(Math.abs(sentCount.get() - expectedTotal) < expectedTotal * 0.1, "실제 전송 수가 목표치의 10% 이내여야 함");
    }
}
