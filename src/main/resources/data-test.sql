-- 테스트 사용자 데이터
-- Rate Limiting 테스트를 위한 기본 사용자들

-- 익명 사용자들 (Rate Limiting 테스트용)
INSERT INTO users (user_id, client_identifier, nickname, email, password_hash, user_type, created_at, last_active_at, is_account_upgraded)
VALUES 
    ('test_user_anon_1', 'test_client_anon_1', '익명테스터1', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('test_user_anon_2', 'test_client_anon_2', '익명테스터2', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('test_user_anon_3', 'test_client_anon_3', '익명테스터3', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 인증 사용자들 (Rate Limiting 테스트용)
-- 비밀번호는 모두 "password123" (BCrypt 해시)
INSERT INTO users (user_id, client_identifier, nickname, email, password_hash, user_type, created_at, last_active_at, email_verified_at, is_account_upgraded)
VALUES 
    ('test_user_auth_1', 'test_client_auth_1', '인증테스터1', 'test1@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('test_user_auth_2', 'test_client_auth_2', '인증테스터2', 'test2@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('test_user_auth_3', 'test_client_auth_3', '인증테스터3', 'test3@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1);

-- WebSocket 디버깅용 사용자
INSERT INTO users (user_id, client_identifier, nickname, email, password_hash, user_type, created_at, last_active_at, is_account_upgraded)
VALUES 
    ('debug_user', 'debug_client', '디버그유저', 'debug@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 메시지 수신용 테스트 유저
INSERT INTO users (user_id, client_identifier, nickname, email, password_hash, user_type, created_at, last_active_at, is_account_upgraded)
VALUES 
    ('test_receiver', 'test_receiver_client', '수신자테스터', 'receiver@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Rate Limiting 버스트 테스트용 사용자
INSERT INTO users (user_id, client_identifier, nickname, email, password_hash, user_type, created_at, last_active_at, is_account_upgraded)
VALUES 
    ('burst_anon_ws', 'burst_anon_client', 'Burst익명테스터', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('burst_auth_ws', 'burst_auth_client', 'Burst인증테스터', 'burst@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1);

-- 복구 테스트용 사용자
INSERT INTO users (user_id, client_identifier, nickname, email, password_hash, user_type, created_at, last_active_at, is_account_upgraded)
VALUES 
    ('recovery_test_user', 'recovery_test_client', 'Recovery테스터', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 연결 테스트용 사용자
INSERT INTO users (user_id, client_identifier, nickname, email, password_hash, user_type, created_at, last_active_at, is_account_upgraded)
VALUES
    ('connection_test', 'connection_test_client', '연결테스터', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 동시 부하 테스트용 사용자 (0~49번까지 50명)
INSERT INTO users (user_id, client_identifier, nickname, email, password_hash, user_type, created_at, last_active_at, is_account_upgraded) VALUES
('concurrent_user_0', 'concurrent_client_0', '동시부하테스터0', 'concurrent0@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_1', 'concurrent_client_1', '동시부하테스터1', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_2', 'concurrent_client_2', '동시부하테스터2', 'concurrent2@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_3', 'concurrent_client_3', '동시부하테스터3', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_4', 'concurrent_client_4', '동시부하테스터4', 'concurrent4@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_5', 'concurrent_client_5', '동시부하테스터5', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_6', 'concurrent_client_6', '동시부하테스터6', 'concurrent6@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_7', 'concurrent_client_7', '동시부하테스터7', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_8', 'concurrent_client_8', '동시부하테스터8', 'concurrent8@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_9', 'concurrent_client_9', '동시부하테스터9', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_10', 'concurrent_client_10', '동시부하테스터10', 'concurrent10@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_11', 'concurrent_client_11', '동시부하테스터11', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_12', 'concurrent_client_12', '동시부하테스터12', 'concurrent12@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_13', 'concurrent_client_13', '동시부하테스터13', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_14', 'concurrent_client_14', '동시부하테스터14', 'concurrent14@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_15', 'concurrent_client_15', '동시부하테스터15', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_16', 'concurrent_client_16', '동시부하테스터16', 'concurrent16@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_17', 'concurrent_client_17', '동시부하테스터17', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_18', 'concurrent_client_18', '동시부하테스터18', 'concurrent18@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_19', 'concurrent_client_19', '동시부하테스터19', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_20', 'concurrent_client_20', '동시부하테스터20', 'concurrent20@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_21', 'concurrent_client_21', '동시부하테스터21', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_22', 'concurrent_client_22', '동시부하테스터22', 'concurrent22@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_23', 'concurrent_client_23', '동시부하테스터23', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_24', 'concurrent_client_24', '동시부하테스터24', 'concurrent24@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_25', 'concurrent_client_25', '동시부하테스터25', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_26', 'concurrent_client_26', '동시부하테스터26', 'concurrent26@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_27', 'concurrent_client_27', '동시부하테스터27', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_28', 'concurrent_client_28', '동시부하테스터28', 'concurrent28@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_29', 'concurrent_client_29', '동시부하테스터29', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_30', 'concurrent_client_30', '동시부하테스터30', 'concurrent30@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_31', 'concurrent_client_31', '동시부하테스터31', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_32', 'concurrent_client_32', '동시부하테스터32', 'concurrent32@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_33', 'concurrent_client_33', '동시부하테스터33', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_34', 'concurrent_client_34', '동시부하테스터34', 'concurrent34@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_35', 'concurrent_client_35', '동시부하테스터35', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_36', 'concurrent_client_36', '동시부하테스터36', 'concurrent36@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_37', 'concurrent_client_37', '동시부하테스터37', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_38', 'concurrent_client_38', '동시부하테스터38', 'concurrent38@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_39', 'concurrent_client_39', '동시부하테스터39', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_40', 'concurrent_client_40', '동시부하테스터40', 'concurrent40@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_41', 'concurrent_client_41', '동시부하테스터41', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_42', 'concurrent_client_42', '동시부하테스터42', 'concurrent42@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_43', 'concurrent_client_43', '동시부하테스터43', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_44', 'concurrent_client_44', '동시부하테스터44', 'concurrent44@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_45', 'concurrent_client_45', '동시부하테스터45', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_46', 'concurrent_client_46', '동시부하테스터46', 'concurrent46@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_47', 'concurrent_client_47', '동시부하테스터47', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('concurrent_user_48', 'concurrent_client_48', '동시부하테스터48', 'concurrent48@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUTHENTICATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('concurrent_user_49', 'concurrent_client_49', '동시부하테스터49', NULL, NULL, 'ANONYMOUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
