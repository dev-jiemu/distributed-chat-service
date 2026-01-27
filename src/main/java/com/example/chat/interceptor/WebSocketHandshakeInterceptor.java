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

            String token = httpRequest.getParameter("token");
            boolean isAuthenticated = false;
            String userId = null;

            if (token != null && !token.isEmpty()) {
                try {
                    if (jwtService.validateToken(token)) {
                        userId = jwtService.getUserIdFromToken(token);
                        if (userId != null) {
                            attributes.put("userId", userId);
                            isAuthenticated = true;
                            log.info("WebSocket handshake - authenticated user: {}", userId);
                        }
                    } else {
                        log.warn("WebSocket handshake - invalid token.");
                    }
                } catch (Exception e) {
                    log.warn("WebSocket handshake - token validation failed: {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket handshake - token is missing.");
            }

            attributes.put("authenticated", isAuthenticated);

            log.debug("WebSocket handshake - userId: {}, authenticated: {}",
                    userId, isAuthenticated);
        }

        return true; 
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                               WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 이후 처리
        log.debug("WebSocket handshake completed");
    }
}
