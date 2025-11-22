package com.example.chat.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${app.server-id}")
    private String serverId;
    
    public static final String CHAT_EXCHANGE = "chat.exchange";
    
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }
    
    @Bean
    public Queue chatQueue() {
        return new Queue("chat.queue." + serverId, true);  // durable = true
    }
    
    @Bean
    public Binding binding(Queue chatQueue, TopicExchange chatExchange) {
        // chat.* 패턴으로 라우팅 (chat.server1, chat.server2 등)
        return BindingBuilder.bind(chatQueue)
                .to(chatExchange)
                .with("chat." + serverId);
    }
    
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
