#!/usr/bin/env python3
"""
WebSocket 기반 Rate Limiting 테스트

실제 Java 서버에 WebSocket 연결하여 Rate Limiting을 테스트합니다.

사전 준비:
    pip install websocket-client

사용법:
    docker-compose up -d  # Redis, RabbitMQ, chat-server 실행
    python3 rate_limit_test_websocket.py
"""

import websocket
import json
import time
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from typing import Dict, List


class ChatWebSocketClient:
    """WebSocket STOMP 클라이언트"""
    
    def __init__(self, user_id: str, token: str, server_url="localhost", server_port=8081):
        self.user_id = user_id
        self.token = token
        self.server_url = server_url
        self.server_port = server_port
        self.ws = None
        
        self.success_count = 0
        self.error_count = 0
        self.rate_limited_count = 0
        self.lock = threading.Lock()
        self.connected = False
        self.subscription_id = 0
        
    def connect(self):
        """WebSocket 연결"""
        try:
            import random
            server_id = random.randint(0, 999)
            session_id = ''.join([chr(random.randint(97, 122)) for _ in range(8)])
            
            ws_url = f"ws://{self.server_url}:{self.server_port}/ws-chat/{server_id}/{session_id}/websocket?token={self.token}"
            
            self.ws = websocket.WebSocketApp(
                ws_url,
                on_open=self._on_open,
                on_message=self._on_message,
                on_error=self._on_error,
                on_close=self._on_close
            )
            
            wst = threading.Thread(target=self.ws.run_forever)
            wst.daemon = True
            wst.start()
            
            timeout = 5
            start = time.time()
            while not self.connected and (time.time() - start) < timeout:
                time.sleep(0.1)
            
            if not self.connected:
                raise Exception("Connection timeout")
            
            return True
            
        except Exception as e:
            print(f"❌ 연결 실패 ({self.user_id}): {e}")
            return False
    
    def _on_open(self, ws):
        """연결 성공 콜백"""
        connect_frame = f"CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\x00"
        sockjs_message = json.dumps([connect_frame])
        ws.send(sockjs_message)
    
    def _on_message(self, ws, message):
        """메시지 수신 콜백"""
        try:
            if message.startswith('o'):
                return
            elif message.startswith('h'):
                return
            elif message.startswith('c'):
                self.connected = False
                return
            elif message.startswith('a'):
                messages = json.loads(message[1:])
                for msg in messages:
                    self._handle_stomp_frame(msg)
        except Exception as e:
            print(f"메시지 처리 에러: {e}, message: {message[:200]}")
    
    def _handle_stomp_frame(self, frame):
        """STOMP 프레임 처리"""
        if frame.startswith("CONNECTED"):
            self.connected = True
            self._subscribe_queues()
            
        elif frame.startswith("MESSAGE"):
            self._handle_message_frame(frame)
    
    def _on_error(self, ws, error):
        """에러 콜백"""
        pass
    
    def _on_close(self, ws, close_status_code, close_msg):
        """연결 종료 콜백"""
        self.connected = False
    
    def _subscribe_queues(self):
        """메시지 큐 구독"""
        self.subscription_id += 1
        sub_frame = f"SUBSCRIBE\nid:sub-{self.subscription_id}\ndestination:/user/queue/errors\n\n\x00"
        sockjs_message = json.dumps([sub_frame])
        self.ws.send(sockjs_message)
        
        self.subscription_id += 1
        sub_frame = f"SUBSCRIBE\nid:sub-{self.subscription_id}\ndestination:/user/queue/messages\n\n\x00"
        sockjs_message = json.dumps([sub_frame])
        self.ws.send(sockjs_message)
    
    def _handle_message_frame(self, message):
        """STOMP MESSAGE 프레임 처리"""
        lines = message.split('\n')
        
        # 헤더 파싱
        headers = {}
        body_start = 0
        for i, line in enumerate(lines):
            if line == '':
                body_start = i + 1
                break
            if ':' in line:
                key, value = line.split(':', 1)
                headers[key] = value
        
        # Body 추출
        body = '\n'.join(lines[body_start:]).rstrip('\x00')
        
        # 목적지 확인
        destination = headers.get('destination', '')
        
        if '/queue/errors' in destination:
            # 에러 메시지
            try:
                error_data = json.loads(body)
                if error_data.get('code') == 'RATE_LIMIT_EXCEEDED':
                    with self.lock:
                        self.rate_limited_count += 1
                        self.error_count += 1
                    # print(f"[{self.user_id}] Rate limited!") # 디버깅용
            except:
                with self.lock:
                    self.error_count += 1
        elif '/queue/messages' in destination:
            # 일반 메시지 (성공)
            with self.lock:
                self.success_count += 1
            # print(f"[{self.user_id}] Message received! Total: {self.success_count}") # 디버깅용
    
    def send_message(self, receiver: str = "test_receiver", content: str = "Test message"):
        """메시지 전송"""
        if not self.connected:
            return False
        
        try:
            message = {
                "type": "CHAT",
                "sender": self.user_id,
                "receiver": receiver,
                "content": content,
                "timestamp": datetime.now().isoformat()
            }
            
            body = json.dumps(message)
            frame = f"SEND\ndestination:/app/chat.sendMessage\ncontent-type:application/json\ncontent-length:{len(body)}\n\n{body}\x00"
            sockjs_message = json.dumps([frame])
            
            self.ws.send(sockjs_message)
            return True
            
        except Exception as e:
            with self.lock:
                self.error_count += 1
            return False
    
    def disconnect(self):
        """연결 종료"""
        if self.ws:
            try:
                disconnect_frame = "DISCONNECT\n\n\x00"
                sockjs_message = json.dumps([disconnect_frame])
                self.ws.send(sockjs_message)
                time.sleep(0.1)
                self.ws.close()
            except:
                pass


def test_burst_limit():
    """버스트 한도 테스트"""
    print("\n" + "="*60)
    print("💥 버스트 한도 테스트")
    print("="*60 + "\n")
    
    test_cases = [
        {
            'user_id': 'burst_test_user',
            'attempts': 150,
            'expected_success': 100,  # 버스트 한도
            'description': '버스트 테스트 - 150개 시도 (버스트 100개)'
        }
    ]
    
    for test in test_cases:
        print(f"🔍 {test['description']}")
        
        # 클라이언트 생성 및 연결
        client = ChatWebSocketClient(user_id=test['user_id'])
        
        if not client.connect():
            print("  ❌ 연결 실패\n")
            continue
        
        # 메시지 전송 - 간격 없이 최대한 빠르게
        for i in range(test['attempts']):
            client.send_message(content=f"Burst test message {i+1}")
            # time.sleep(0.01) 제거 - 간격 없이 전송!
        
        # 결과 수집 대기
        time.sleep(2)
        
        # 통계
        total_sent = test['attempts']
        success = client.success_count
        rate_limited = client.rate_limited_count
        
        print(f"  시도: {total_sent}개")
        print(f"  예상 성공: ~{test['expected_success']}개")
        print(f"  실제 성공: {success}개")
        print(f"  차단: {rate_limited}개")
        
        # 검증 (약간의 오차 허용)
        is_valid = (test['expected_success'] - 20 <= success <= test['expected_success'] + 20) and rate_limited > 0
        print(f"  결과: {'✅ PASS' if is_valid else '❌ FAIL'}\n")
        
        # 연결 종료
        client.disconnect()
        time.sleep(0.5)


def test_concurrent_load():
    """동시 부하 테스트"""
    print("\n" + "="*60)
    print("🔥 동시 부하 테스트")
    print("="*60 + "\n")
    
    user_count = 50
    messages_per_user = 100
    
    print(f"동시 사용자: {user_count}명")
    print(f"사용자당 메시지: {messages_per_user}개")
    print(f"총 시도: {user_count * messages_per_user}개\n")
    
    def simulate_user(user_index: int) -> Dict:
        """사용자 시뮬레이션"""
        user_id = f"concurrent_user_{user_index}"
        is_auth = (user_index % 2 == 0)
        
        client = ChatWebSocketClient(
            user_id=user_id,
            is_authenticated=is_auth
        )
        
        if not client.connect():
            return {
                'user_id': user_id,
                'success': 0,
                'rate_limited': 0,
                'errors': 1
            }
        
        # 메시지 전송
        for i in range(messages_per_user):
            client.send_message(content=f"Concurrent test {i+1}")
            time.sleep(0.001)
        
        # 결과 수집 대기
        time.sleep(0.5)
        
        result = {
            'user_id': user_id,
            'success': client.success_count,
            'rate_limited': client.rate_limited_count,
            'errors': client.error_count
        }
        
        client.disconnect()
        return result
    
    start_time = time.time()
    results = []
    
    with ThreadPoolExecutor(max_workers=20) as executor:
        futures = []
        
        for i in range(user_count):
            future = executor.submit(simulate_user, i)
            futures.append(future)
        
        for future in as_completed(futures):
            results.append(future.result())
    
    duration = time.time() - start_time
    
    # 통계
    total_success = sum(r['success'] for r in results)
    total_limited = sum(r['rate_limited'] for r in results)
    total_errors = sum(r['errors'] for r in results)
    total_attempted = user_count * messages_per_user
    
    print("\n📊 결과:")
    print(f"총 시도:        {total_attempted:>8,}개")
    print(f"성공:           {total_success:>8,}개")
    print(f"차단:           {total_limited:>8,}개")
    print(f"기타 에러:      {total_errors - total_limited:>8,}개")
    print(f"차단율:         {(total_limited/total_attempted*100):>8.2f}%")
    print(f"소요 시간:      {duration:>8.2f}초")
    print(f"처리량:         {total_attempted/duration:>8.1f}개/초")


def test_rate_recovery():
    """Rate Limit 복구 테스트"""
    print("\n" + "="*60)
    print("⏱️  Rate Limit 복구 테스트")
    print("="*60 + "\n")
    
    print("🔍 토큰 충전 속도 확인 (익명: 3.33 토큰/초)")
    
    user_id = "recovery_test_user"
    client = ChatWebSocketClient(user_id=user_id, is_authenticated=False)
    
    if not client.connect():
        print("  ❌ 연결 실패\n")
        return
    
    # 1단계: 버스트 한도까지 전송
    print("\n1️⃣  버스트 한도(100개)까지 전송...")
    for i in range(100):
        client.send_message(content=f"Message {i+1}")
        time.sleep(0.001)
    
    time.sleep(0.5)
    phase1_success = client.success_count
    print(f"   성공: {phase1_success}개")
    
    # 2단계: 추가 전송 (차단되어야 함)
    print("\n2️⃣  추가 10개 전송 (차단되어야 함)...")
    for i in range(10):
        client.send_message(content=f"Extra message {i+1}")
        time.sleep(0.001)
    
    time.sleep(0.5)
    phase2_limited = client.rate_limited_count
    print(f"   차단: {phase2_limited}개")
    
    # 3단계: 대기 후 재시도
    wait_time = 3
    print(f"\n3️⃣  {wait_time}초 대기 (토큰 충전 중: ~10개 예상)...")
    time.sleep(wait_time)
    
    prev_success = client.success_count
    for i in range(15):
        client.send_message(content=f"After wait message {i+1}")
        time.sleep(0.001)
    
    time.sleep(0.5)
    phase3_success = client.success_count - prev_success
    phase3_limited = client.rate_limited_count - phase2_limited
    
    print(f"   성공: {phase3_success}개")
    print(f"   차단: {phase3_limited}개")
    
    # 검증
    expected_recovered = int(wait_time * 3.33)  # 익명 사용자 충전 속도
    is_valid = (expected_recovered - 3 <= phase3_success <= expected_recovered + 3)
    
    print(f"\n결과: {'✅ PASS' if is_valid else '❌ FAIL'}")
    print(f"  예상 복구: ~{expected_recovered}개")
    print(f"  실제 복구: {phase3_success}개\n")
    
    client.disconnect()


def test_simple_connection():
    """간단한 연결 테스트"""
    print("\n" + "="*60)
    print("🔌 서버 연결 테스트")
    print("="*60 + "\n")
    
    print("WebSocket 서버에 연결 시도 중...")
    
    client = ChatWebSocketClient(user_id="connection_test", is_authenticated=False)
    
    if client.connect():
        print("✅ 연결 성공!")
        print(f"   사용자: {client.user_id}")
        print(f"   서버: ws://{client.server_url}:{client.server_port}/ws-chat")
        
        # 테스트 메시지 전송
        print("\n테스트 메시지 10개 전송 중...")
        for i in range(10):
            client.send_message(content=f"Test message {i+1}")
            time.sleep(0.01)
        
        time.sleep(0.5)
        
        print(f"\n결과:")
        print(f"  성공: {client.success_count}개")
        print(f"  에러: {client.error_count}개")
        print(f"  차단: {client.rate_limited_count}개")
        
        client.disconnect()
        print("\n✅ 연결 종료")
    else:
        print("❌ 연결 실패")
        print("\n문제 해결:")
        print("  1. docker-compose up -d 로 서버가 실행 중인지 확인")
        print("  2. 포트 8081이 사용 중인지 확인: lsof -i :8081")
        print("  3. 서버 로그 확인: docker-compose logs chat-server")


def main():
    """메인 실행"""
    print("\n" + "="*60)
    print("🚀 WebSocket Rate Limiting 테스트")
    print("="*60)
    
    print("\n⚙️  테스트 항목:")
    print("   1. 서버 연결 테스트")
    print("   2. 버스트 한도 테스트")
    print("   3. Rate Limit 복구 테스트")
    print("   4. 동시 부하 테스트")
    
    print("\n📝 주의: docker-compose up으로 서버가 실행 중이어야 합니다!")
    print("   서버 URL: ws://localhost:8081/ws-chat\n")
    
    try:
        # 1. 연결 테스트
        test_simple_connection()
        time.sleep(1)
        
        # 2. 버스트 한도 테스트
        test_burst_limit()
        time.sleep(1)
        
        # 3. 복구 테스트
        test_rate_recovery()
        time.sleep(1)
        
        # 4. 동시 부하 테스트
        choice = input("\n동시 부하 테스트를 실행하시겠습니까? (y/n): ")
        if choice.lower() == 'y':
            test_concurrent_load()
        
        print("\n" + "="*60)
        print("✅ 모든 테스트 완료!")
        print("="*60 + "\n")
        
    except KeyboardInterrupt:
        print("\n\n⚠️  테스트 중단됨")
    except Exception as e:
        print(f"\n\n❌ 테스트 실패: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
