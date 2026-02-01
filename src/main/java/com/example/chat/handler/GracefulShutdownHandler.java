package com.example.chat.handler;

import com.example.chat.service.ConnectionService;
import com.example.chat.service.ServerHeartbeatService;
import com.example.chat.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;



// Pod가 정상종료될 때 (SIGTERM 수신 시) 세션과 하트비트를 정리하는 핸들러.
/**
 * Graceful Shutdown과 SIGKILL의 차이
 * - SIGTERM (정상종료) → 이 핸들러가 실행되어 깔끔하게 정리됨
 * - SIGKILL (강제종료) → 이 핸들러가 실행되지 않음 → 하트비트 감지가 백업으로 작동
 */
@Component
public class GracefulShutdownHandler implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownHandler.class);

    private final SessionManager sessionManager;
    private final ConnectionService connectionService;
    private final ServerHeartbeatService heartbeatService;

    @Value("${app.server-id}")
    private String serverId;

    public GracefulShutdownHandler(SessionManager sessionManager,
                                   ConnectionService connectionService,
                                   ServerHeartbeatService heartbeatService) {
        this.sessionManager = sessionManager;
        this.connectionService = connectionService;
        this.heartbeatService = heartbeatService;
    }

    @Override
    public void destroy() {
        log.info("[GracefulShutdown] SIGTERM received. Starting cleanup for server: {}", serverId);

        // 1. 하트비트 즉시 중지
        heartbeatService.stopHeartbeat();
        log.info("[GracefulShutdown] Heartbeat stopped for server: {}", serverId);

        // 2. 현재 서버의 모든 유저 세션 정리
        Set<String> userIds = sessionManager.getAllLocalUserIds();
        log.info("[GracefulShutdown] Cleaning up {} sessions...", userIds.size());

        for (String userId : userIds) {
            connectionService.removeUserConnection(userId);
            log.debug("[GracefulShutdown] Removed connection for user: {}", userId);
        }

        log.info("[GracefulShutdown] Cleanup completed for server: {}", serverId);
    }
}
