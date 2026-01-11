package com.example.chat.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WebSocketHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            // 쿼리 파라미터에서 userId 추출
            String userId = httpRequest.getParameter("userId");
            if (userId != null) {
                attributes.put("userId", userId);
                log.info("WebSocket handshake - userId: {}", userId);
            }
            
            // 추가 인증 로직을 여기에 구현할 수 있습니다
            // 예: JWT 토큰 검증, 세션 확인 등
            
            // IP 주소 저장
            String remoteAddress = httpRequest.getRemoteAddr();
            attributes.put("remoteAddress", remoteAddress);
        }
        
        return true; // true를 반환하면 연결 허용
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                               WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 이후 처리
        log.debug("WebSocket handshake completed");
    }
}
