package com.example.chat.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${server.id:server1}")
    private String serverId;

    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String ROUTING_KEY_PREFIX = "chat.";

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }

    @Bean
    public Queue chatQueue() {
        // 각 서버마다 고유한 큐를 생성
        return new Queue("chat.queue." + serverId, true);
    }

    @Bean
    public Binding binding(Queue chatQueue, TopicExchange chatExchange) {
        // 자신의 서버 ID로 라우팅된 메시지를 받음
        return BindingBuilder.bind(chatQueue)
                .to(chatExchange)
                .with(ROUTING_KEY_PREFIX + serverId);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
