package com.chat.consume;
import com.chat.session.SessionManager;
import com.example.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "chat.queue.#{systemEnvironment['SERVER_ID']}")
    public void handleMessage(String messageJson) {
        try {
            ChatMessage message = objectMapper.readValue(messageJson, ChatMessage.class);

            WebSocketSession session = sessionManager.getLocalSession(message.getReceiverId());

            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(messageJson));
            }
        } catch (Exception e) {
            log.error("메시지 전송 실패", e);
        }
    }
}
