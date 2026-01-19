# 📦 Redis Keys Documentation

1. [Rate Limiting 키](#1-rate-limiting-키)
2. [사용자 연결 관리 키](#2-사용자-연결-관리-키)
3. [세션 관리 키](#3-세션-관리-키)
4. [채팅방 관리 키](#4-채팅방-관리-키)
5. [메시지 히스토리 키](#5-메시지-히스토리-키)
6. [사용자 상태 관리 키](#6-사용자-상태-관리-키)
7. [서버 관리 키](#7-서버-관리-키)

---

## 1. Rate Limiting 키

### 📌 익명 사용자 Rate Limit
```
키 패턴: rate:anon:{userId}
데이터 타입: String (JSON)
TTL: 300초 (5분)
관리 클래스: RateLimitingService
```

**값 구조:**
```json
{
  "tokens": 95.5,
  "maxTokens": 100,
  "refillRate": 3.333333,
  "lastRefillTime": "2024-01-18T10:30:00"
}
```

**설명:**
- 익명 사용자의 Token Bucket Rate Limit 정보
- 분당 200개 메시지, 버스트 100개
- 충전 속도: 200/60 = 3.33 토큰/초

**예시:**
```
rate:anon:user_123
rate:anon:192.168.1.100
```

---

### 📌 인증 사용자 Rate Limit
```
키 패턴: rate:auth:{userId}
데이터 타입: String (JSON)
TTL: 300초 (5분)
관리 클래스: RateLimitingService
```

**값 구조:**
```json
{
  "tokens": 450.0,
  "maxTokens": 100,
  "refillRate": 8.333333,
  "lastRefillTime": "2024-01-18T10:30:00"
}
```

**설명:**
- 인증된 사용자의 Token Bucket Rate Limit 정보
- 분당 500개 메시지, 버스트 100개
- 충전 속도: 500/60 = 8.33 토큰/초

**예시:**
```
rate:auth:john_doe
rate:auth:alice_2024
```

---

## 2. 사용자 연결 관리 키

### 📌 사용자 연결 정보
```
키 패턴: connection:{userId}
데이터 타입: Object (UserConnection)
TTL: 30분
관리 클래스: ConnectionService
```

**값 구조:**
```json
{
  "userId": "john_doe",
  "sessionId": "abc123session",
  "serverId": "server1",
  "connectedAt": 1705584600000
}
```

**설명:**
- 사용자가 현재 어느 서버에 연결되어 있는지 추적
- WebSocket 연결 시 저장, 끊김 시 삭제
- 메시지 라우팅에 사용

**예시:**
```
connection:john_doe
connection:alice_2024
```

---

## 3. 세션 관리 키

### 📌 사용자-서버 연결 맵핑
```
키 패턴: user:connections
데이터 타입: Hash
필드: {userId} → {serverId}
TTL: 없음
관리 클래스: SessionManager
```

**값 구조:**
```
HGETALL user:connections
{
  "john_doe": "server1",
  "alice_2024": "server2",
  "bob_smith": "server1"
}
```

**설명:**
- 각 사용자가 어느 서버에 연결되어 있는지 빠르게 조회
- Hash 타입으로 O(1) 조회 성능

---

### 📌 세션 ID 저장
```
키 패턴: session:{serverId}:{userId}
데이터 타입: String
TTL: 30분
관리 클래스: SessionManager
```

**값 구조:**
```
GET session:server1:john_doe
→ "abc123session"
```

**설명:**
- 서버별 사용자의 WebSocket 세션 ID
- TTL로 자동 만료 (비활성 세션 정리)

**예시:**
```
session:server1:john_doe
session:server2:alice_2024
```

---

## 4. 채팅방 관리 키

### 📌 채팅방 멤버 목록
```
키 패턴: room:members:{roomId}
데이터 타입: Set
TTL: 없음
관리 클래스: ChatRoomService
```

**값 구조:**
```
SMEMBERS room:members:room_123
→ ["john_doe", "alice_2024", "bob_smith"]
```

**설명:**
- 특정 채팅방에 속한 멤버들의 userId 목록
- Set 타입으로 중복 방지 및 빠른 조회

**예시:**
```
room:members:room_general
room:members:room_tech_talk
room:members:room_123
```

---

### 📌 사용자가 속한 채팅방 목록
```
키 패턴: user:rooms:{userId}
데이터 타입: Set
TTL: 없음
관리 클래스: ChatRoomService
```

**값 구조:**
```
SMEMBERS user:rooms:john_doe
→ ["room_general", "room_tech_talk", "room_project_a"]
```

**설명:**
- 특정 사용자가 속한 모든 채팅방 목록
- 사용자가 참여 중인 채팅방 빠르게 조회

**예시:**
```
user:rooms:john_doe
user:rooms:alice_2024
```

---

## 5. 메시지 히스토리 키

### 📌 1:1 대화 메시지 히스토리
```
키 패턴: messages:{user1}:{user2}
데이터 타입: Sorted Set
TTL: 없음
관리 클래스: MessageHistoryService
```

**값 구조:**
```
ZRANGE messages:alice:john 0 -1 WITHSCORES
→ [
  {"id":"msg1","sender":"alice","receiver":"john","content":"Hi","timestamp":"2024-01-18T10:00:00"},
  1705584000,
  {"id":"msg2","sender":"john","receiver":"alice","content":"Hello","timestamp":"2024-01-18T10:01:00"},
  1705584060
]
```

**설명:**
- 두 사용자 간의 1:1 대화 히스토리
- 키는 알파벳 순으로 정렬 (alice:john = john:alice)
- Score는 Unix timestamp (시간순 정렬)
- 최대 100개까지 저장 (오래된 메시지 자동 삭제)

**예시:**
```
messages:alice:john
messages:bob:charlie
```

---

### 📌 채팅방 메시지 히스토리
```
키 패턴: room:messages:{roomId}
데이터 타입: Sorted Set
TTL: 없음
관리 클래스: MessageHistoryService
```

**값 구조:**
```
ZRANGE room:messages:room_general 0 -1 WITHSCORES
→ [
  {"id":"msg1","roomId":"room_general","sender":"alice","content":"Hello everyone"},
  1705584000,
  {"id":"msg2","roomId":"room_general","sender":"john","content":"Hi Alice!"},
  1705584060
]
```

**설명:**
- 특정 채팅방의 메시지 히스토리
- Score는 Unix timestamp
- 최대 100개까지 저장

**예시:**
```
room:messages:room_general
room:messages:room_tech_talk
```

---

## 6. 사용자 상태 관리 키

### 📌 사용자 온라인 상태
```
키 패턴: presence:{userId}
데이터 타입: Hash
TTL: 5분
관리 클래스: UserPresenceService
```

**값 구조:**
```
HGETALL presence:john_doe
{
  "status": "online",
  "server": "server1",
  "lastSeen": "1705584600000"
}
```

**설명:**
- 사용자의 현재 온라인 상태
- 5분 TTL로 자동 만료 (자동 오프라인 처리)
- 주기적인 하트비트로 TTL 갱신

**예시:**
```
presence:john_doe
presence:alice_2024
```

---

## 7. 서버 관리 키

### 📌 활성 서버 목록
```
키 패턴: active:servers
데이터 타입: Set
TTL: 60초
관리 클래스: UserPresenceService
```

**값 구조:**
```
SMEMBERS active:servers
→ ["server1", "server2", "server3"]
```

**설명:**
- 현재 살아있는 서버들의 목록
- 각 서버가 30초마다 하트비트 전송
- 60초 TTL로 죽은 서버 자동 제거
- 죽은 서버의 세션 정리에 사용

---

## 📊 Redis 키 사용 통계

### 키 타입별 분류
```
String (JSON): 3개
  - rate:anon:*
  - rate:auth:*
  - session:*

Hash: 2개
  - user:connections
  - presence:*

Set: 3개
  - room:members:*
  - user:rooms:*
  - active:servers

Sorted Set: 2개
  - messages:*
  - room:messages:*

Object: 1개
  - connection:*
```

### TTL 설정 요약
```
5분 (300초):
  - rate:anon:*
  - rate:auth:*
  - presence:*

30분 (1800초):
  - connection:*
  - session:*

60초:
  - active:servers

TTL 없음 (영구):
  - user:connections
  - room:members:*
  - user:rooms:*
  - messages:*
  - room:messages:*
```

---

## 🔍 Redis 키 조회 명령어

### 전체 키 패턴별 조회
```bash
# Redis CLI 접속
docker-compose exec redis redis-cli -a test

# Rate Limiting 키
KEYS rate:anon:*
KEYS rate:auth:*

# 연결 관리 키
KEYS connection:*
KEYS session:*
HGETALL user:connections

# 채팅방 키
KEYS room:members:*
KEYS user:rooms:*

# 메시지 히스토리 키
KEYS messages:*
KEYS room:messages:*

# 상태 관리 키
KEYS presence:*
SMEMBERS active:servers
```

### 특정 키 상세 조회
```bash
# Rate Limit 정보
GET rate:anon:user_123
GET rate:auth:john_doe

# 연결 정보
GET connection:john_doe
HGET user:connections john_doe

# 채팅방 멤버
SMEMBERS room:members:room_general

# 메시지 히스토리 (최근 10개)
ZREVRANGE messages:alice:john 0 9 WITHSCORES

# 사용자 상태
HGETALL presence:john_doe
TTL presence:john_doe
```

---

## 🧹 Redis 데이터 정리

### 개발/테스트용 전체 삭제
```bash
# 주의: 모든 데이터 삭제
redis-cli -a test FLUSHDB

# 패턴별 삭제
redis-cli -a test --scan --pattern "rate:*" | xargs redis-cli -a test DEL
redis-cli -a test --scan --pattern "connection:*" | xargs redis-cli -a test DEL
```

### 특정 사용자 데이터 삭제
```bash
# 사용자 john_doe의 모든 데이터 삭제
redis-cli -a test DEL rate:anon:john_doe
redis-cli -a test DEL rate:auth:john_doe
redis-cli -a test DEL connection:john_doe
redis-cli -a test HDEL user:connections john_doe
redis-cli -a test DEL presence:john_doe

# 사용자가 속한 모든 채팅방에서 제거
redis-cli -a test SMEMBERS user:rooms:john_doe | while read room; do
  redis-cli -a test SREM room:members:$room john_doe
done
redis-cli -a test DEL user:rooms:john_doe
```

---

## 📈 모니터링 쿼리

### Redis 메모리 사용량
```bash
# 전체 메모리 정보
redis-cli -a test INFO memory

# 키 개수
redis-cli -a test DBSIZE

# 패턴별 키 개수
redis-cli -a test --scan --pattern "rate:*" | wc -l
redis-cli -a test --scan --pattern "connection:*" | wc -l
redis-cli -a test --scan --pattern "messages:*" | wc -l
```

### 성능 모니터링
```bash
# 실시간 명령어 모니터링
redis-cli -a test MONITOR

# 느린 쿼리 로그
redis-cli -a test SLOWLOG GET 10

# 현재 연결 수
redis-cli -a test CLIENT LIST
```

---

## 🎯 최적화 가이드

### 1. Rate Limiting 최적화
```yaml
# application.yml에서 조정
app:
  rate-limit:
    token-ttl: 300  # TTL 조정으로 메모리 사용량 제어
```

### 2. 메시지 히스토리 최적화
```java
// MessageHistoryService.java
private static final int MAX_HISTORY_SIZE = 100;  // 필요에 따라 조정
```

### 3. 세션 TTL 최적화
```java
// SessionManager.java
private static final long SESSION_TTL_MINUTES = 30;  // 비활성 세션 정리 시간
```

### 4. Redis 메모리 정책
```bash
# redis.conf 또는 docker-compose.yml
maxmemory 256mb
maxmemory-policy allkeys-lru  # LRU로 오래된 키 자동 삭제
```

---

## 🔒 고려사항

### 1. Rate Limiting 키
- ⚠️ IP 주소가 키에 포함될 수 있음 (개인정보 보호 필요)
- 💡 해시 처리 고려: `rate:anon:hash(192.168.1.100)`

### 2. 메시지 히스토리 키
- ⚠️ 민감한 메시지 내용이 Redis에 저장됨
- 💡 암호화 저장 고려
- 💡 주기적인 백업 및 삭제 정책 필요
