package com.example.chat.listener;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.MessageRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQListener {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQListener.class);

    private final MessageRoutingService messageRoutingService;

    public RabbitMQListener(MessageRoutingService messageRoutingService) {
        this.messageRoutingService = messageRoutingService;
    }

    @RabbitListener(queues = "#{chatQueue.name}")
    public void handleMessage(ChatMessage message) {
        log.info("Received message from RabbitMQ: {} -> {}", message.getSender(), message.getReceiver());
        messageRoutingService.deliverMessageToLocalUser(message);
    }
}
