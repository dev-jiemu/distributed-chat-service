package com.example.chat.service;

import com.example.chat.config.RabbitMQConfig;
import com.example.chat.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageRoutingService {
    
    private static final Logger log = LoggerFactory.getLogger(MessageRoutingService.class);
    
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConnectionService connectionService;

    public MessageRoutingService(RabbitTemplate rabbitTemplate, SimpMessagingTemplate messagingTemplate, ConnectionService connectionService) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingTemplate = messagingTemplate;
        this.connectionService = connectionService;
    }

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
        // 개인 메시지 큐로 전달
        messagingTemplate.convertAndSendToUser(
            message.getReceiver(), 
            "/queue/messages", 
            message
        );
        log.info("Message delivered to local user {}", message.getReceiver());
    }
}
