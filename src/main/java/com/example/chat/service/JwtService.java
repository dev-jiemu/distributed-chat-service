package com.example.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /* TODO : 구현예정
        generateAccessToken(String userId): 액세스 토큰 생성
        generateRefreshToken(String userId): 리프레시 토큰 생성
        validateToken(String token): 토큰 유효성 검증
        getUserIdFromToken(String token): 토큰에서 userId 추출
        isTokenExpired(String token): 토큰 만료 여부 확인
     */
}
