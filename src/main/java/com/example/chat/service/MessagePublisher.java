package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisher {
    
    private final MessageRoutingService messageRoutingService;
    
    public void publishMessage(ChatMessage message) {
        // 메시지 ID와 타임스탬프 설정
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        
        // 1:1 메시지인 경우
        if (message.getReceiver() != null && !message.getReceiver().isEmpty()) {
            messageRoutingService.routeMessage(message);
        } 
        // 브로드캐스트 메시지인 경우 (JOIN, LEAVE 등)
        else if (message.getType() == ChatMessage.MessageType.JOIN ||
                 message.getType() == ChatMessage.MessageType.LEAVE) {
            // TODO: 브로드캐스트 구현
            log.info("Broadcast message: {} - {}", message.getType(), message.getSender());
        }
        // 그룹 채팅 메시지인 경우
        else if (message.getRoomId() != null) {
            // TODO: 룸 메시지 구현
            log.info("Room message: {} - {}", message.getRoomId(), message.getSender());
        }
    }
}
