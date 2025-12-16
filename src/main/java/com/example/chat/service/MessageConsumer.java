package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {
    
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    
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
        String messageJson = objectMapper.writeValueAsString(message);
        
        // 1:1 메시지인 경우
        if (message.getReceiverId() != null) {
            sessionManager.sendMessageToLocalUser(message.getReceiverId(), messageJson);
        }
        // 룸 메시지인 경우
        else if (message.getRoomId() != null) {
            // TODO: 룸에 속한 로컬 사용자들에게 메시지 전송
            log.info("Room message handling not implemented yet");
        }
    }
    
    private void handleSystemMessage(ChatMessage message) throws Exception {
        // JOIN/LEAVE 메시지는 보통 브로드캐스트
        String messageJson = objectMapper.writeValueAsString(message);
        
        // 현재 서버에 연결된 모든 사용자에게 전송 (발신자 제외)
        // TODO: 실제로는 더 효율적인 방법 필요
        log.info("System message: {} - {}", message.getType(), message.getContent());
    }
    
    private void handleTypingMessage(ChatMessage message) throws Exception {
        // 타이핑 중 표시
        if (message.getReceiverId() != null) {
            String messageJson = objectMapper.writeValueAsString(message);
            sessionManager.sendMessageToLocalUser(message.getReceiverId(), messageJson);
        }
    }
    
    private void handleReadMessage(ChatMessage message) throws Exception {
        // 읽음 확인 처리
        if (message.getReceiverId() != null) {
            String messageJson = objectMapper.writeValueAsString(message);
            sessionManager.sendMessageToLocalUser(message.getReceiverId(), messageJson);
        }
    }
}
