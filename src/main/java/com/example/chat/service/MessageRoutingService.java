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
    private final ServerHeartbeatService heartbeatService;
    private final String serverId;

    public MessageRoutingService(RabbitTemplate rabbitTemplate, 
                                SimpMessagingTemplate messagingTemplate, 
                                ConnectionService connectionService,
                                ServerHeartbeatService heartbeatService) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingTemplate = messagingTemplate;
        this.connectionService = connectionService;
        this.heartbeatService = heartbeatService;
        this.serverId = System.getenv().getOrDefault("HOSTNAME", "server1");
    }

    // 라우팅 전 서버 alive check
    public void routeMessage(ChatMessage message) {
        String receiverServer = connectionService.getUserServer(message.getReceiver());
        
        if (receiverServer == null) {
            log.warn("User {} not found in any server", message.getReceiver());
            return;
        }

        // 같은 서버에 있으면 직접 전달 (하트비트 확인 불필요)
        if (receiverServer.equals(serverId)) {
            deliverMessageToLocalUser(message);
            log.info("Message from {} to {} delivered locally on server {}", 
                    message.getSender(), message.getReceiver(), serverId);
            return;
        }

        // 다른 서버로의 라우팅 시 하트비트 확인
        if (!heartbeatService.isServerAlive(receiverServer)) {
            log.warn("Target server {} is dead. Invalidating session for user {}",
                    receiverServer, message.getReceiver());
            connectionService.removeUserConnection(message.getReceiver());
            return;
        }

        // 대상 서버가 살아있으면 RabbitMQ를 통해 전달
        String routingKey = RabbitMQConfig.ROUTING_KEY_PREFIX + receiverServer;
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, routingKey, message);
        log.info("Message from {} to {} routed to server {}", 
                message.getSender(), message.getReceiver(), receiverServer);
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
