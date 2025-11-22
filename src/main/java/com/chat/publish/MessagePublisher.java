package com.chat.publish;

import com.example.chat.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    public void publishMessage(ChatMessage message) {
        String targetServer = (String) redisTemplate.opsForHash().get("user:connections", message.getReceiverId());

        if (targetServer != null) {
            // 특정 서버로 라우팅
            String routingKey = "chat." + targetServer;
            rabbitTemplate.convertAndSend("chat.exchange", routingKey, message);
        }
    }
}