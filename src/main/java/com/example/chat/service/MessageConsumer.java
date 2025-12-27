package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @RabbitListener(queues = "#{chatQueue.name}")  // 동적으로 큐 이름 바인딩
    public void handleMessage(ChatMessage message) {
        try {
            log.debug("Message received from queue: {}", message);
            
            // 메시지 타입에 따라 처리
            switch (message.getType()) {
                case CHAT:
                    handleChatMessageDto(message);
                    break;
                case JOIN:
                case LEAVE:
                    handleSystemMessage(message);
                    break;
                case TYPING:
                    handleTypingMessage(message);
                    break;
                case READ:
                    handleReadMessage(message);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Error handling message", e);
        }
    }
    
    private void handleChatMessageDto(ChatMessage message) throws Exception {
        // 1:1 메시지인 경우
        if (message.getReceiver() != null && message.getRoomId() == null) {
            // 받는 사람에게 전송
            messagingTemplate.convertAndSendToUser(
                message.getReceiver(), 
                "/queue/messages", 
                message
            );
            
            // sender와 receiver가 다른 경우만 보낸 사람에게도 메세지 전송
            if (!message.getSender().equals(message.getReceiver())) {
                messagingTemplate.convertAndSendToUser(
                    message.getSender(), 
                    "/queue/messages", 
                    message
                );
            }
            
            log.info("1:1 message sent from {} to {}", message.getSender(), message.getReceiver());
        } else if (message.getRoomId() != null) { // 룸 메시지인 경우
            // 룸 토픽으로 브로드캐스트
            messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);
            log.info("Room message sent to room: {}", message.getRoomId());
        } else { // 브로드캐스트 메시지인 경우
            // 전체 공개 토픽으로 브로드캐스트
            messagingTemplate.convertAndSend("/topic/public", message);
            log.info("Broadcast message sent");
        }
    }
    
    private void handleSystemMessage(ChatMessage message) throws Exception {
        // JOIN/LEAVE 메시지는 public 토픽으로 브로드캐스트
        messagingTemplate.convertAndSend("/topic/public", message);
        log.info("System message broadcast: {} - {}", message.getType(), message.getContent());
    }
    
    private void handleTypingMessage(ChatMessage message) throws Exception {
        // 타이핑 중 표시
        if (message.getReceiver() != null) {
            messagingTemplate.convertAndSendToUser(
                message.getReceiver(), 
                "/queue/typing", 
                message
            );
        }
    }
    
    private void handleReadMessage(ChatMessage message) throws Exception {
        // 읽음 확인 처리
        if (message.getReceiver() != null) {
            messagingTemplate.convertAndSendToUser(
                message.getReceiver(), 
                "/queue/read", 
                message
            );
        }
    }
}
