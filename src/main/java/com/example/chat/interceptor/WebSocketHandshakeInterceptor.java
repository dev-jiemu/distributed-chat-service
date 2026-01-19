package com.example.chat.interceptor;

import com.example.chat.service.JwtService;
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
    
    private final JwtService jwtService;
    
    public WebSocketHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

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
            
            // JWT 토큰 확인 (인증 여부 판단)
            String token = httpRequest.getParameter("token");
            boolean isAuthenticated = false;
            
            if (token != null && !token.isEmpty()) {
                try {
                    // JWT 토큰 검증
                    if (jwtService.validateToken(token)) {
                        String userIdFromToken = jwtService.getUserIdFromToken(token);
                        
                        // 토큰에서 추출한 userId와 파라미터의 userId가 일치하는지 확인
                        if (userIdFromToken != null && userIdFromToken.equals(userId)) {
                            isAuthenticated = true;
                            log.info("WebSocket handshake - authenticated user: {}", userId);
                        } else {
                            log.warn("WebSocket handshake - token userId mismatch: {} vs {}", userIdFromToken, userId);
                        }
                    } else {
                        log.warn("WebSocket handshake - invalid token for user: {}", userId);
                    }
                } catch (Exception e) {
                    log.warn("WebSocket handshake - token validation failed: {}", e.getMessage());
                }
            }
            
            // 인증 여부를 세션 속성에 저장 (Rate Limiting에서 사용)
            attributes.put("authenticated", isAuthenticated);
            
            // IP 주소 저장
            String remoteAddress = httpRequest.getRemoteAddr();
            attributes.put("remoteAddress", remoteAddress);
            
            log.debug("WebSocket handshake - userId: {}, authenticated: {}, IP: {}", 
                userId, isAuthenticated, remoteAddress);
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
