package com.example.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        
        String requestURI = request.getRequestURI();
        
        // 정적 리소스나 공개 엔드포인트는 로그 레벨 낮춤
        if (isPublicResource(requestURI)) {
            log.debug("공개 리소스 접근 (인증 불필요): {}", requestURI);
        } else {
            log.warn("인증되지 않은 요청: {} - {}", requestURI, authException.getMessage());
        }
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "인증이 필요합니다.");
        body.put("path", request.getServletPath());
        
        objectMapper.writeValue(response.getOutputStream(), body);
    }
    
    private boolean isPublicResource(String uri) {
        return uri.endsWith(".css") || 
               uri.endsWith(".js") || 
               uri.endsWith(".ico") ||
               uri.endsWith(".png") ||
               uri.endsWith(".jpg") ||
               uri.endsWith(".html") ||
               uri.startsWith("/api/auth/") ||
               uri.startsWith("/ws-chat");
    }
}
