package com.gloud.auth.controller;


import com.gloud.auth.dto.MemberDto;
import com.gloud.auth.dto.TokenDto;
import com.gloud.auth.security.CustomUserDetails;
import com.gloud.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> Register(@RequestBody MemberDto memberDto) throws InterruptedException {
        authService.Register(memberDto);

        return new ResponseEntity<>("Register Success", HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<MemberDto> getCurrentUser() {
        log.info("/auth/me");
        // SecurityContextHolder에서 Authentication 객체 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 권한 정보 확인
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        String username = userDetails.getMemberName();

        MemberDto memberDto = MemberDto.builder()
                .email(email)
                .username(username)
                .build();

        return ResponseEntity.ok(memberDto);


    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken() {
        log.info("/auth/validate");
        // SecurityContextHolder에서 Authentication 객체 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        // 권한 정보 확인
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // 권한 정보를 사용하여 요청 처리
        return new ResponseEntity<>(email, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenDto> reissueToken(HttpServletRequest request, HttpServletResponse response) {
        String csrfHeader = request.getHeader("X-CSRF-Token");
        String csrfCookie = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "csrfToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (csrfHeader == null || !csrfHeader.equals(csrfCookie)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        TokenDto tokenDto = authService.reissueToken(request, response);
        return ResponseEntity.ok(tokenDto);
    }
}
