package com.example.chat.config;

import com.example.chat.interceptor.WebSocketHandshakeInterceptor;
import com.example.chat.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserInterceptor userInterceptor;

    @Value("${spring.rabbitmq.stomp.host:localhost}")
    private String stompHost;

    @Value("${spring.rabbitmq.stomp.port:61613}")
    private int stompPort;

    @Value("${spring.rabbitmq.stomp.login:admin}")
    private String stompLogin;

    @Value("${spring.rabbitmq.stomp.passcode:admin}")
    private String stompPasscode;

    public WebSocketConfig(JwtService jwtService, UserInterceptor userInterceptor) {
        this.jwtService = jwtService;
        this.userInterceptor = userInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // StompBrokerRelay 사용 : 대용량 트래픽 처리를 위해 외부 메시지 브로커(RabbitMQ) 사용
        config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(stompHost)                     // RabbitMQ 호스트
                .setRelayPort(stompPort)                     // STOMP 포트 (RabbitMQ의 STOMP 플러그인 포트)
                .setClientLogin(stompLogin)                  // RabbitMQ 사용자명
                .setClientPasscode(stompPasscode)            // RabbitMQ 비밀번호
                .setSystemLogin(stompLogin)                  // 시스템 로그인
                .setSystemPasscode(stompPasscode)            // 시스템 비밀번호
                .setSystemHeartbeatSendInterval(10000)       // 시스템 하트비트 전송 간격 (10초)
                .setSystemHeartbeatReceiveInterval(10000);   // 시스템 하트비트 수신 간격 (10초)

        // 클라이언트에서 메시지를 보낼 때 사용할 prefix
        config.setApplicationDestinationPrefixes("/app");
        // 사용자별 큐를 위한 prefix 설정
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 등록 (SockJS 지원)
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new WebSocketHandshakeInterceptor(jwtService))
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // UserInterceptor 등록 - STOMP CONNECT 시 userId를 Principal로 설정
        registration.interceptors(userInterceptor);
    }
}
