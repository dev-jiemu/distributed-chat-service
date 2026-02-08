# 인증기능 부하테스트 기록 정리
- 사용자 회원가입(`_api/auth/register`) 엔드포인트의 동시성 처리 성능을 측정
- 데이터베이스 커넥션 풀 및 관련 설정 변경에 따른 성능 변화를 확인합니다.

## 실행 방법

1.  **서버 및 관련 서비스 시작**

    ```bash
    docker-compose up --build
    ```

2.  **테스트 스크립트 의존성 설치**

    ```bash
    pip install -r requirements.txt
    ```

3.  **부하 테스트 스크립트 실행**

    ```bash
    python3 auth_load_test.py
    ```

## 테스트 결과

```text
============================================================
🚀 Auth Service Load Test
============================================================
URL: http://localhost:8081/api/auth/register
Total Requests: 100
Concurrency Level: 20
------------------------------------------------------------

📊 Test Results:
------------------------------------------------------------
Total time taken: 2.35 seconds
Requests per second (RPS): 42.49
✅ Successful registrations: 100
⚠️ Conflicts (already registered): 0
⏳ Timeouts: 0
============================================================

👍 Performance seems acceptable.
```