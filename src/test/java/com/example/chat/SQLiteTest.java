package com.example.chat;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RequiredArgsConstructor
public class SQLiteTest {

    private final UserRepository userRepository;

    @Test
    public void generateTestUsers() {
        if (userRepository.count() == 0) {
            List<User> testUsers = new ArrayList<>();

            for (int i = 1; i <= 100; i++) {
                User user = new User();
                user.setUserId("test_user_" + i);
                user.setClientIdentifier("test_client_" + i);
                user.setNickname("TestUser" + i);
                user.setIpAddress("192.168.1." + (i % 255));
                user.setUserAgent("TestAgent/1.0");
                user.setCreatedAt(LocalDateTime.now());
                testUsers.add(user);
            }

            userRepository.saveAll(testUsers);
            System.out.println("success test user create");
        }
    }
}
