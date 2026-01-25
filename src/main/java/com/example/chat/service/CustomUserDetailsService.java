package com.example.chat.service;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        return new CustomUserDetails(user);
    }

    public static class CustomUserDetails implements UserDetails {
        
        private final User user;
        
        public CustomUserDetails(User user) {
            this.user = user;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));
            return authorities;
        }
        
        @Override
        public String getPassword() {
            return user.getPasswordHash();
        }
        
        @Override
        public String getUsername() {
            return user.getUserId();
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return true;
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return true;
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }
        
        @Override
        public boolean isEnabled() {
            // 이메일 인증 여부 등에 따라 설정 가능
            return true;
        }
        
        // 추가 메서드 - 실제 User 엔터티 접근용
        public User getUser() {
            return user;
        }
        
        public String getUserId() {
            return user.getUserId();
        }
        
        public String getNickname() {
            return user.getNickname();
        }
    }
}
