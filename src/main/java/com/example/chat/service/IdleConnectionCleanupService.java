package com.example.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

/*
 * Idle WebSocket 연결 정리 스케줄러
 * - 일정 시간 동안 아무 활동이 없는 세션을 주기적으로 탐지해서 강제 종료
 * - 좀비 커넥션으로 인한 리소스 낭비 방지
 */
@Service
public class IdleConnectionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(IdleConnectionCleanupService.class);

    private final SessionManager sessionManager;

    @Value("${app.connection.idle-timeout-minutes:10}")
    private int idleTimeoutMinutes;     // 이 시간 동안 활동 없으면 idle로 판단

    public IdleConnectionCleanupService(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /*
     * 주기적으로 idle 세션 탐지 및 제거
     * 기본 5분마다 실행
     */
    @Scheduled(fixedRateString = "${app.connection.cleanup-interval-ms:300000}")
    public void cleanupIdleSessions() {
        long idleThresholdMillis = (long) idleTimeoutMinutes * 60 * 1000;
        Set<String> idleUserIds = sessionManager.getIdleUserIds(idleThresholdMillis);

        if (idleUserIds.isEmpty()) {
            log.debug("Idle session cleanup - no idle sessions found");
            return;
        }

        log.info("Idle session cleanup started - found {} idle sessions (threshold: {}min)",
                idleUserIds.size(), idleTimeoutMinutes);

        int closed = 0;
        for (String userId : idleUserIds) {
            WebSocketSession session = sessionManager.getLocalSession(userId);

            if (session == null) {
                // 세션 객체가 없으면 활동 기록만 정리
                sessionManager.removeSession(userId);
                continue;
            }

            if (!session.isOpen()) {
                // 이미 닫힌 세션이면 정리만
                sessionManager.removeSession(userId);
                continue;
            }

            try {
                session.close(CloseStatus.SESSION_NOT_RELIABLE); // 명시적 종료
                sessionManager.removeSession(userId);
                closed++;
                log.info("Idle session closed - userId: {}, sessionId: {}", userId, session.getId());
            } catch (Exception e) {
                log.warn("Failed to close idle session - userId: {}, error: {}", userId, e.getMessage());
            }
        }

        log.info("Idle session cleanup completed - closed: {}/{}", closed, idleUserIds.size());
    }
}
