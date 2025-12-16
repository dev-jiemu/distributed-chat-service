package com.example.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributedChatServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistributedChatServiceApplication.class, args);
    }
}

