package com.example.chat.service;

import com.example.chat.config.RabbitMQConfig;
import com.example.chat.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final SessionManager sessionManager;
    
    public void publishMessage(ChatMessage message) {
        // 메시지 ID와 타임스탬프 설정
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        
        // 1:1 메시지인 경우
        if (message.getReceiverId() != null) {
            publishDirectMessage(message);
        } 
        // 브로드캐스트 메시지인 경우 (JOIN, LEAVE 등)
        else if (message.getType() == ChatMessage.MessageType.JOIN || 
                 message.getType() == ChatMessage.MessageType.LEAVE) {
            publishBroadcastMessage(message);
        }
        // 그룹 채팅 메시지인 경우
        else if (message.getRoomId() != null) {
            publishRoomMessage(message);
        }
    }
    
    private void publishDirectMessage(ChatMessage message) {
        String targetServer = sessionManager.getUserServer(message.getReceiverId());
        
        if (targetServer != null) {
            String routingKey = "chat." + targetServer;
            rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, routingKey, message);
            
            log.info("Direct message published - From: {}, To: {}, TargetServer: {}", 
                    message.getSenderId(), message.getReceiverId(), targetServer);
        } else {
            log.warn("Target user not online - UserId: {}", message.getReceiverId());
        }
    }
    
    private void publishBroadcastMessage(ChatMessage message) {
        // 모든 서버로 메시지 전송 (fanout 방식)
        // 실제로는 별도의 fanout exchange를 사용하는 것이 좋습니다
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, "chat.broadcast", message);
        log.info("Broadcast message published - Type: {}, From: {}", 
                message.getType(), message.getSenderId());
    }
    
    private void publishRoomMessage(ChatMessage message) {
        // 룸 멤버들이 속한 서버로 메시지 전송
        // TODO: ChatRoomService에서 룸 멤버 조회 후 각 서버로 전송
        String routingKey = "chat.room." + message.getRoomId();
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, routingKey, message);
        
        log.info("Room message published - RoomId: {}, From: {}", 
                message.getRoomId(), message.getSenderId());
    }
}
