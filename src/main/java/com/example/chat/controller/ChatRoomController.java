package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.ChatRoomService;
import com.example.chat.service.ConnectionService;
import com.example.chat.service.MessageHistoryService;
import com.example.chat.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {
    
    private final ChatRoomService chatRoomService;
    private final ConnectionService connectionService;
    private final UserPresenceService userPresenceService;
    private final MessageHistoryService messageHistoryService;
    
    // 채팅방 생성
    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Object>> createRoom(
            @RequestParam String roomName,
            @RequestParam String userId) {
        
        String roomId = "room_" + System.currentTimeMillis();
        chatRoomService.joinRoom(roomId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("roomName", roomName);
        response.put("createdBy", userId);
        
        return ResponseEntity.ok(response);
    }
    
    // 채팅방 참가
    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<Map<String, Object>> joinRoom(
            @PathVariable String roomId,
            @RequestParam String userId) {
        
        chatRoomService.joinRoom(roomId, userId);
        Set<Object> members = chatRoomService.getRoomMembers(roomId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("userId", userId);
        response.put("members", members);
        
        return ResponseEntity.ok(response);
    }
    
    // 채팅방 나가기
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<Map<String, Object>> leaveRoom(
            @PathVariable String roomId,
            @RequestParam String userId) {
        
        chatRoomService.leaveRoom(roomId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("userId", userId);
        response.put("message", "Successfully left the room");
        
        return ResponseEntity.ok(response);
    }
    
    // 채팅방 멤버 조회
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<Set<Object>> getRoomMembers(@PathVariable String roomId) {
        return ResponseEntity.ok(chatRoomService.getRoomMembers(roomId));
    }
    
    // 사용자가 속한 채팅방 목록 조회
    @GetMapping("/users/{userId}/rooms")
    public ResponseEntity<Set<Object>> getUserRooms(@PathVariable String userId) {
        return ResponseEntity.ok(chatRoomService.getUserRooms(userId));
    }
    
    // 온라인 사용자 상태 조회
    @GetMapping("/users/{userId}/status")
    public ResponseEntity<Map<String, Object>> getUserStatus(@PathVariable String userId) {
        boolean isOnline = userPresenceService.isUserOnline(userId);
        String status = userPresenceService.getUserStatus(userId);
        String server = connectionService.getUserServer(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("online", isOnline);
        response.put("status", status);
        response.put("server", server);
        
        return ResponseEntity.ok(response);
    }
    
    // 1:1 메시지 히스토리 조회
    @GetMapping("/messages/direct")
    public ResponseEntity<List<ChatMessage>> getDirectMessageHistory(
            @RequestParam String user1,
            @RequestParam String user2,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<ChatMessage> messages = messageHistoryService.getDirectMessageHistory(user1, user2, limit);
        return ResponseEntity.ok(messages);
    }
    
    // 채팅방 메시지 히스토리 조회
    @GetMapping("/messages/room/{roomId}")
    public ResponseEntity<List<ChatMessage>> getRoomMessageHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<ChatMessage> messages = messageHistoryService.getRoomMessageHistory(roomId, limit);
        return ResponseEntity.ok(messages);
    }
    
    // 서버 상태 조회 (health check)
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("serverId", System.getenv("SERVER_ID"));
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }
}
