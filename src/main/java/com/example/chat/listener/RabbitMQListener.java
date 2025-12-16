package com.example.chat.listener;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.MessageRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQListener {
    
    private final MessageRoutingService messageRoutingService;
    
    @RabbitListener(queues = "#{chatQueue.name}")
    public void handleMessage(ChatMessage message) {
        log.info("Received message from RabbitMQ: {} -> {}", message.getSender(), message.getReceiver());
        messageRoutingService.deliverMessageToLocalUser(message);
    }
}
