#!/usr/bin/env python3

import pika
import json
import time
import threading
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

def send_messages(thread_id, message_count):
    """각 스레드에서 메시지를 전송하는 함수"""
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host='localhost',
            port=5672,
            credentials=pika.PlainCredentials('admin', 'admin')
        )
    )
    channel = connection.channel()
    
    success_count = 0
    error_count = 0
    
    for i in range(message_count):
        try:
            message = {
                'roomId': f'room-{thread_id % 5}',
                'senderId': f'user-{thread_id}',
                'content': f'Message {i} from thread {thread_id}',
                'timestamp': int(time.time() * 1000)
            }
            
            channel.basic_publish(
                exchange='chat.exchange',
                routing_key=f"chat.room.{message['roomId']}",
                body=json.dumps(message)
            )
            success_count += 1
        except Exception as e:
            error_count += 1
            print(f"Error in thread {thread_id}: {e}")
    
    connection.close()
    return success_count, error_count

def main():
    print("=== RabbitMQ 부하테스트 (Python) ===")
    print("주의: docker-compose up -d rabbitmq 를 먼저 실행하세요!\n")
    
    # 테스트 파라미터
    num_threads = 10
    messages_per_thread = 1000
    total_messages = num_threads * messages_per_thread
    
    print(f"스레드 수: {num_threads}")
    print(f"스레드당 메시지: {messages_per_thread}")
    print(f"총 메시지: {total_messages}\n")
    
    # 연결 테스트
    try:
        test_conn = pika.BlockingConnection(
            pika.ConnectionParameters(
                host='localhost',
                port=5672,
                credentials=pika.PlainCredentials('admin', 'admin')
            )
        )
        test_channel = test_conn.channel()
        test_channel.exchange_declare(exchange='chat.exchange', exchange_type='topic', durable=True)
        test_conn.close()
        print("RabbitMQ 연결 성공!\n")
    except Exception as e:
        print(f"RabbitMQ 연결 실패: {e}")
        print("docker-compose up -d rabbitmq 명령을 실행했는지 확인하세요.")
        return
    
    # 부하테스트 실행
    start_time = time.time()
    
    with ThreadPoolExecutor(max_workers=num_threads) as executor:
        futures = []
        for i in range(num_threads):
            future = executor.submit(send_messages, i, messages_per_thread)
            futures.append(future)
        
        # 결과 수집
        total_success = 0
        total_error = 0
        for future in futures:
            success, error = future.result()
            total_success += success
            total_error += error
    
    duration = time.time() - start_time
    
    # 결과 출력
    print("\n=== 테스트 결과 ===")
    print(f"총 메시지: {total_messages}")
    print(f"성공: {total_success}")
    print(f"실패: {total_error}")
    print(f"소요 시간: {duration:.2f}초")
    print(f"처리량: {total_success / duration:.2f} msg/sec")

if __name__ == "__main__":
    main()
