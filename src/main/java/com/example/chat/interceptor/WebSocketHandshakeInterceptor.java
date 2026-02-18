package com.example.chat.interceptor;

import com.example.chat.service.ConnectionRateLimitService;
import com.example.chat.service.JwtService;
import com.example.chat.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    private final ConnectionRateLimitService connectionRateLimitService;
    private final SessionManager sessionManager;

    public WebSocketHandshakeInterceptor(JwtService jwtService,
                                         ConnectionRateLimitService connectionRateLimitService,
                                         SessionManager sessionManager) {
        this.jwtService = jwtService;
        this.connectionRateLimitService = connectionRateLimitService;
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // 서버 최대 연결 수 체크
            if (sessionManager.isConnectionLimitExceeded()) {
                log.warn("Handshake rejected - server connection limit exceeded");
                response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                return false;
            }

            // IP 기준 연결 Rate Limit 체크
            String clientIp = getClientIp(httpRequest);
            if (!connectionRateLimitService.allowConnection(clientIp)) {
                log.warn("Handshake rejected - connection rate limit exceeded for IP: {}", clientIp);
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return false;
            }

            // JWT 인증
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
            log.debug("WebSocket handshake - userId: {}, authenticated: {}", userId, isAuthenticated);
        }

        return true;
    }

    // IP 추출 : X-Forwarded-For 헤더에서 추출
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                               WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 이후 처리
        log.debug("WebSocket handshake completed");
    }
}
