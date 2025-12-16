package com.example.chat.service;

import com.example.chat.config.RabbitMQConfig;
import com.example.chat.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRoutingService {
    
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConnectionService connectionService;
    
    /**
     * 메시지를 적절한 서버로 라우팅
     */
    public void routeMessage(ChatMessage message) {
        String receiverServer = connectionService.getUserServer(message.getReceiver());
        
        if (receiverServer != null) {
            // 수신자가 다른 서버에 연결되어 있으면 RabbitMQ를 통해 전달
            String routingKey = RabbitMQConfig.ROUTING_KEY_PREFIX + receiverServer;
            rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, routingKey, message);
            log.info("Message from {} to {} routed to server {}", 
                    message.getSender(), message.getReceiver(), receiverServer);
        } else {
            log.warn("User {} not found in any server", message.getReceiver());
        }
    }
    
    /**
     * 로컬 사용자에게 메시지 전달
     */
    public void deliverMessageToLocalUser(ChatMessage message) {
        String destination = "/queue/messages/" + message.getReceiver();
        messagingTemplate.convertAndSend(destination, message);
        log.info("Message delivered to local user {}", message.getReceiver());
    }
}
