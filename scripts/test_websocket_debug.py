#!/usr/bin/env python3
"""
WebSocket 디버깅 스크립트 - 실제 메시지 형식 확인
"""

import websocket
import json
import time
import random
import threading

received_messages = []

def on_message(ws, message):
    print(f"\n[수신] {message}")
    received_messages.append(message)

def on_error(ws, error):
    print(f"\n[에러] {error}")

def on_close(ws, close_status_code, close_msg):
    print(f"\n[종료] {close_status_code}: {close_msg}")

def on_open(ws):
    print("[연결 성공]")
    
    def send_messages():
        # STOMP CONNECT (userId 헤더 추가)
        connect_frame = f"CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\nuserId:{user_id}\n\n\x00"
        sockjs_message = json.dumps([connect_frame])
        print(f"\n[전송 CONNECT] {sockjs_message}")
        ws.send(sockjs_message)
        
        time.sleep(1)  # 1초 대기
        
        # 사용자 추가
        add_user_msg = {
            "type": "JOIN",
            "sender": "debug_user",
            "timestamp": "2026-01-25T00:00:00"
        }
        body = json.dumps(add_user_msg)
        frame = f"SEND\ndestination:/app/chat.addUser\ncontent-type:application/json\ncontent-length:{len(body)}\n\n{body}\x00"
        sockjs_message = json.dumps([frame])
        print(f"\n[전송 ADD_USER] {sockjs_message}")
        ws.send(sockjs_message)
        
        time.sleep(1)  # 1초 대기
        
        # 구독
        sub_frame = f"SUBSCRIBE\nid:sub-1\ndestination:/user/queue/messages\n\n\x00"
        sockjs_message = json.dumps([sub_frame])
        print(f"\n[전송 SUBSCRIBE] {sockjs_message}")
        ws.send(sockjs_message)
        
        time.sleep(1)  # 1초 대기
        
        # 메시지 전송
        chat_msg = {
            "type": "CHAT",
            "sender": "debug_user",
            "receiver": "test_receiver",
            "content": "Test message",
            "timestamp": "2026-01-25T00:00:00"
        }
        body = json.dumps(chat_msg)
        frame = f"SEND\ndestination:/app/chat.sendMessage\ncontent-type:application/json\ncontent-length:{len(body)}\n\n{body}\x00"
        sockjs_message = json.dumps([frame])
        print(f"\n[전송 SEND_MESSAGE] {sockjs_message}")
        ws.send(sockjs_message)
        
        # 10초 대기 후 종료
        print("\n[10초 대기 중... 응답 확인]")
        time.sleep(10)
        
        print(f"\n[총 수신 메시지 수: {len(received_messages)}]")
        ws.close()
    
    # 별도 스레드에서 메시지 전송
    threading.Thread(target=send_messages, daemon=True).start()

if __name__ == "__main__":
    websocket.enableTrace(True)  # 상세 로그 활성화
    
    server_id = random.randint(0, 999)
    session_id = ''.join([chr(random.randint(97, 122)) for _ in range(8)])
    user_id = "debug_user"
    
    # userId를 쿼리 파라미터로 추가
    ws_url = f"ws://localhost:8081/ws-chat/{server_id}/{session_id}/websocket?userId={user_id}"
    
    print(f"연결 URL: {ws_url}")
    
    ws = websocket.WebSocketApp(
        ws_url,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close
    )
    
    ws.run_forever()
