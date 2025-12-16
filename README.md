# distributed-chat-service
채팅 서비스를 대용량 처리로 연습해보기

### 아키텍쳐
```mermaid
graph TB
    subgraph "Client Layer"
        C1[Client 1]
        C2[Client 2]
        C3[Client 3]
        C4[Client N...]
    end
    
    subgraph "Load Balancer"
        LB[Nginx/Spring Cloud Gateway<br/>Sticky Session]
    end
    
    subgraph "Application Servers"
        S1[Chat Server 1<br/>WebSocket Handler]
        S2[Chat Server 2<br/>WebSocket Handler]
        S3[Chat Server 3<br/>WebSocket Handler]
    end
    
    subgraph "Message Broker"
        RMQ[(RabbitMQ)]
        EX[Topic Exchange<br/>chat.exchange]
        Q1[Queue<br/>chat.queue.server1]
        Q2[Queue<br/>chat.queue.server2]
        Q3[Queue<br/>chat.queue.server3]
    end
    
    subgraph "Cache Layer"
        Redis[(Redis)]
        R1[User Connection Info]
        R2[User Presence]
        R3[Chat Room Info]
    end
    
    C1 & C2 -->|WebSocket| LB
    C3 & C4 -->|WebSocket| LB
    
    LB -->|Sticky Session| S1
    LB -->|Sticky Session| S2
    LB -->|Sticky Session| S3
    
    S1 & S2 & S3 -->|Publish| RMQ
    
    RMQ --> EX
    EX -->|Routing| Q1
    EX -->|Routing| Q2
    EX -->|Routing| Q3
    
    Q1 -->|Consume| S1
    Q2 -->|Consume| S2
    Q3 -->|Consume| S3
    
    S1 & S2 & S3 <-->|R/W| Redis
    
    style C1 fill:#e1f5fe
    style C2 fill:#e1f5fe
    style C3 fill:#e1f5fe
    style C4 fill:#e1f5fe
    style LB fill:#fff3e0
    style S1 fill:#e8f5e9
    style S2 fill:#e8f5e9
    style S3 fill:#e8f5e9
    style RMQ fill:#f3e5f5
    style Redis fill:#ffebee
```


### Service flow
```mermaid
sequenceDiagram
    participant User1 as User 1<br/>(Server 1)
    participant Server1 as Chat Server 1
    participant Redis as Redis
    participant RabbitMQ as RabbitMQ
    participant Server2 as Chat Server 2
    participant User2 as User 2<br/>(Server 2)
    
    Note over User1,User2: 채팅 메시지 전송 프로세스
    
    User1->>Server1: WebSocket 연결
    Server1->>Redis: 연결 정보 저장<br/>(user1 -> server1)
    
    User2->>Server2: WebSocket 연결
    Server2->>Redis: 연결 정보 저장<br/>(user2 -> server2)
    
    User1->>Server1: 채팅 메시지 전송<br/>(to: User2)
    Server1->>Redis: User2 서버 위치 조회
    Redis-->>Server1: Server2
    
    Server1->>RabbitMQ: 메시지 발행<br/>(routing key: chat.server2)
    RabbitMQ->>Server2: 메시지 전달<br/>(via Queue)
    Server2->>User2: WebSocket으로<br/>메시지 전송
    
    Note over User1,User2: 응답 메시지
    User2->>Server2: 응답 메시지
    Server2->>Redis: User1 서버 위치 조회
    Server2->>RabbitMQ: 메시지 발행<br/>(routing key: chat.server1)
    RabbitMQ->>Server1: 메시지 전달
    Server1->>User1: WebSocket으로<br/>메시지 전송
```

### Stacks
- Language: Java 17
- Framework: Spring Boot, Spring WebSocket
- Message Broker: RabbitMQ
-  Cache: Redis
   Load Balancer: Nginx / Spring Cloud Gateway
- Protocol: WebSocket (with SockJS fallback)



---

### Memo
Next
- JWT 기반 인증 추가
- 파일 업로드 지원
- 읽음 확인 기능
- 타이핑 표시 기능
- 푸시 알림
- 메시지 암호화
- 데이터베이스 연동
- Kubernetes / Helm chart 배포 설정
