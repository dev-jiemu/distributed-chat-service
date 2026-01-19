#!/usr/bin/env python3
"""
HTTP 기반 Rate Limiting 테스트 (간단 버전)

Redis에 직접 연결하여 Rate Limit 상태를 확인하고,
여러 클라이언트를 시뮬레이션합니다.

사용법:
    docker-compose up -d  # Redis, RabbitMQ, chat-server 실행
    pip install redis requests
    python rate_limit_test_simple.py
"""

import redis
import json
import time
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from typing import Dict, List
import random


class RateLimitChecker:
    """Redis를 통한 Rate Limit 상태 확인"""
    
    def __init__(self, redis_host='localhost', redis_port=6379, redis_password='test'):
        """Redis 연결 초기화"""
        try:
            # Redis 연결 (비밀번호가 있을 경우와 없을 경우 모두 처리)
            if redis_password:
                self.redis_client = redis.Redis(
                    host=redis_host,
                    port=redis_port,
                    password=redis_password,
                    decode_responses=True,
                    socket_connect_timeout=5
                )
            else:
                self.redis_client = redis.Redis(
                    host=redis_host,
                    port=redis_port,
                    decode_responses=True,
                    socket_connect_timeout=5
                )
            
            # 연결 테스트
            self.redis_client.ping()
            print("✅ Redis 연결 성공")
            print(f"   호스트: {redis_host}:{redis_port}")
            if redis_password:
                print(f"   비밀번호: {'*' * len(redis_password)}")
        except redis.exceptions.AuthenticationError as e:
            print(f"❌ Redis 인증 실패: {e}")
            print(f"   비밀번호를 확인하세요. 현재 설정: {redis_password}")
            raise
        except redis.exceptions.ConnectionError as e:
            print(f"❌ Redis 연결 실패: {e}")
            print(f"   Redis가 실행 중인지 확인하세요: docker-compose ps")
            raise
        except Exception as e:
            print(f"❌ Redis 연결 실패: {e}")
            raise
    
    def get_rate_limit_info(self, user_id: str, is_authenticated: bool = False) -> Dict:
        """사용자의 Rate Limit 정보 조회"""
        prefix = "rate:auth:" if is_authenticated else "rate:anon:"
        key = f"{prefix}{user_id}"
        
        try:
            data = self.redis_client.get(key)
            if data:
                return json.loads(data)
            return None
        except Exception as e:
            print(f"❌ Rate Limit 정보 조회 실패: {e}")
            return None
    
    def reset_rate_limit(self, user_id: str, is_authenticated: bool = False):
        """사용자의 Rate Limit 초기화"""
        prefix = "rate:auth:" if is_authenticated else "rate:anon:"
        key = f"{prefix}{user_id}"
        self.redis_client.delete(key)
    
    def get_all_rate_limits(self) -> Dict[str, Dict]:
        """모든 Rate Limit 정보 조회"""
        results = {}
        
        # 익명 사용자
        for key in self.redis_client.keys("rate:anon:*"):
            user_id = key.replace("rate:anon:", "")
            data = self.redis_client.get(key)
            if data:
                results[f"anon:{user_id}"] = json.loads(data)
        
        # 인증 사용자
        for key in self.redis_client.keys("rate:auth:*"):
            user_id = key.replace("rate:auth:", "")
            data = self.redis_client.get(key)
            if data:
                results[f"auth:{user_id}"] = json.loads(data)
        
        return results


class MessageSimulator:
    """메시지 전송 시뮬레이터"""
    
    def __init__(self, checker: RateLimitChecker):
        self.checker = checker
        self.success_count = 0
        self.error_count = 0
        self.rate_limited_count = 0
    
    def simulate_message(self, user_id: str, is_authenticated: bool) -> bool:
        """
        메시지 전송 시뮬레이션
        실제로는 Redis의 Rate Limit 정보를 기반으로 판단
        """
        info = self.checker.get_rate_limit_info(user_id, is_authenticated)
        
        # 정보가 없으면 새 사용자 (버스트 만큼 토큰 있음)
        if info is None:
            self.success_count += 1
            return True
        
        # 토큰 계산 (실제 서비스와 동일한 로직)
        tokens = info.get('tokens', 0)
        
        if tokens >= 1.0:
            self.success_count += 1
            return True
        else:
            self.rate_limited_count += 1
            self.error_count += 1
            return False


def test_rate_limit_visualization():
    """Rate Limit 시각화 테스트"""
    print("\n" + "="*60)
    print("📊 Rate Limit 상태 모니터링")
    print("="*60 + "\n")
    
    checker = RateLimitChecker()
    
    # 테스트 사용자 생성
    test_users = [
        ('test_user_1', False),
        ('test_user_2', True),
        ('test_user_3', False),
    ]
    
    print("사용자별 Rate Limit 상태:\n")
    
    for user_id, is_auth in test_users:
        info = checker.get_rate_limit_info(user_id, is_auth)
        
        user_type = "인증" if is_auth else "익명"
        
        if info:
            tokens = info.get('tokens', 0)
            max_tokens = info.get('maxTokens', 0)
            refill_rate = info.get('refillRate', 0)
            
            # 진행 바 생성
            bar_length = 30
            filled = int((tokens / max_tokens) * bar_length) if max_tokens > 0 else 0
            bar = "█" * filled + "░" * (bar_length - filled)
            
            print(f"{user_id} ({user_type})")
            print(f"  토큰: [{bar}] {tokens:.2f}/{max_tokens}")
            print(f"  충전 속도: {refill_rate:.2f} 토큰/초")
            print()
        else:
            print(f"{user_id} ({user_type})")
            print(f"  상태: 데이터 없음 (아직 메시지 전송 안함)")
            print()


def test_burst_limit():
    """버스트 한도 테스트"""
    print("\n" + "="*60)
    print("💥 버스트 한도 테스트")
    print("="*60 + "\n")
    
    checker = RateLimitChecker()
    
    test_cases = [
        {
            'user_id': 'burst_anon',
            'is_auth': False,
            'attempts': 150,
            'expected_success': 100,  # 버스트 한도
            'description': '익명 사용자 - 150개 시도 (버스트 100개)'
        },
        {
            'user_id': 'burst_auth',
            'is_auth': True,
            'attempts': 150,
            'expected_success': 100,
            'description': '인증 사용자 - 150개 시도 (버스트 100개)'
        }
    ]
    
    for test in test_cases:
        print(f"🔍 {test['description']}")
        
        # 초기화
        checker.reset_rate_limit(test['user_id'], test['is_auth'])
        
        # 첫 메시지로 Rate Limit 정보 생성 (실제 서비스에서)
        # 여기서는 Redis에 수동으로 초기 상태 설정
        prefix = "rate:auth:" if test['is_auth'] else "rate:anon:"
        key = f"{prefix}{test['user_id']}"
        
        initial_data = {
            'tokens': test['expected_success'],  # 버스트
            'maxTokens': test['expected_success'],
            'refillRate': 500/60 if test['is_auth'] else 200/60,  # 초당 충전 비율
            'lastRefillTime': datetime.now().isoformat()
        }
        
        checker.redis_client.set(key, json.dumps(initial_data), ex=300)
        
        # 시뮬레이션
        simulator = MessageSimulator(checker)
        
        for i in range(test['attempts']):
            simulator.simulate_message(test['user_id'], test['is_auth'])
        
        print(f"  시도: {test['attempts']}개")
        print(f"  예상 성공: ~{test['expected_success']}개")
        print(f"  실제 성공: {simulator.success_count}개")
        print(f"  차단: {simulator.rate_limited_count}개")
        
        # 검증
        is_valid = abs(simulator.success_count - test['expected_success']) <= 10
        print(f"  결과: {'✅ PASS' if is_valid else '❌ FAIL'}\n")


def test_concurrent_load():
    """동시 부하 테스트"""
    print("\n" + "="*60)
    print("🔥 동시 부하 테스트")
    print("="*60 + "\n")
    
    checker = RateLimitChecker()
    user_count = 50
    messages_per_user = 100
    
    print(f"동시 사용자: {user_count}명")
    print(f"사용자당 메시지: {messages_per_user}개")
    print(f"총 시도: {user_count * messages_per_user}개\n")
    
    # 모든 사용자 초기화
    for i in range(user_count):
        user_id = f"concurrent_user_{i}"
        is_auth = (i % 2 == 0)
        checker.reset_rate_limit(user_id, is_auth)
    
    def simulate_user(user_id: str, is_auth: bool) -> Dict:
        """사용자 시뮬레이션"""
        simulator = MessageSimulator(checker)
        
        for _ in range(messages_per_user):
            simulator.simulate_message(user_id, is_auth)
            time.sleep(0.001)  # 1ms 딜레이
        
        return {
            'user_id': user_id,
            'success': simulator.success_count,
            'rate_limited': simulator.rate_limited_count
        }
    
    start_time = time.time()
    results = []
    
    with ThreadPoolExecutor(max_workers=20) as executor:
        futures = []
        
        for i in range(user_count):
            user_id = f"concurrent_user_{i}"
            is_auth = (i % 2 == 0)
            
            future = executor.submit(simulate_user, user_id, is_auth)
            futures.append(future)
        
        for future in as_completed(futures):
            results.append(future.result())
    
    duration = time.time() - start_time
    
    # 통계
    total_success = sum(r['success'] for r in results)
    total_limited = sum(r['rate_limited'] for r in results)
    total_attempted = user_count * messages_per_user
    
    print("\n📊 결과:")
    print(f"총 시도:        {total_attempted:>8,}개")
    print(f"성공:           {total_success:>8,}개")
    print(f"차단:           {total_limited:>8,}개")
    print(f"성공률:         {(total_success/total_attempted*100):>8.2f}%")
    print(f"소요 시간:      {duration:>8.2f}초")


def monitor_rate_limits_realtime(duration: int = 10):
    """실시간 Rate Limit 모니터링"""
    print("\n" + "="*60)
    print("📡 실시간 Rate Limit 모니터링")
    print(f"   {duration}초간 모니터링")
    print("="*60 + "\n")
    
    checker = RateLimitChecker()
    start_time = time.time()
    
    while (time.time() - start_time) < duration:
        # 화면 클리어 (선택사항)
        print("\033[H\033[J", end="")
        
        print(f"⏱️  경과 시간: {time.time() - start_time:.1f}초\n")
        
        all_limits = checker.get_all_rate_limits()
        
        if not all_limits:
            print("📭 활성 사용자 없음\n")
        else:
            print(f"👥 활성 사용자: {len(all_limits)}명\n")
            
            for user_key, info in list(all_limits.items())[:10]:  # 최대 10명만 표시
                tokens = info.get('tokens', 0)
                max_tokens = info.get('maxTokens', 0)
                
                bar_length = 20
                filled = int((tokens / max_tokens) * bar_length) if max_tokens > 0 else 0
                bar = "█" * filled + "░" * (bar_length - filled)
                
                print(f"{user_key:30s} [{bar}] {tokens:>6.2f}/{max_tokens}")
        
        time.sleep(1)


def main():
    """메인 실행"""
    print("\n" + "="*60)
    print("🚀 Rate Limiting 테스트 (Redis 직접 연결)")
    print("="*60)
    
    print("\n⚙️  테스트 항목:")
    print("   1. Rate Limit 상태 시각화")
    print("   2. 버스트 한도 테스트")
    print("   3. 동시 부하 테스트")
    print("   4. 실시간 모니터링 (선택)")
    
    print("\n📝 주의: docker-compose up으로 Redis가 실행 중이어야 합니다!")
    print("   Redis 비밀번호: test")
    print("   Redis 포트: 6379\n")
    
    try:
        # 테스트 실행
        test_rate_limit_visualization()
        time.sleep(1)
        
        test_burst_limit()
        time.sleep(1)
        
        test_concurrent_load()
        
        # 실시간 모니터링 선택
        choice = input("\n실시간 모니터링을 시작하시겠습니까? (y/n): ")
        if choice.lower() == 'y':
            monitor_rate_limits_realtime(duration=20)
        
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
