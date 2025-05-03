package com.gloud.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gloud.auth.dto.TokenDto;
import com.gloud.auth.repository.MemberRepository;
import com.gloud.auth.repository.RedisTokenRepository;
import com.gloud.auth.util.CookieUtil;
import com.gloud.auth.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RedisTokenRepository redisTokenRepository;
    private final CookieUtil cookieUtil;

    @Value("${spring.oauth2.response.redirect-uri}")
    private String REDIRECT_URI;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttribute("email");
        Long memberId = Long.parseLong(oAuth2User.getAttribute("memberId").toString());
        String role = (String) oAuth2User.getAttribute("role");


        // JWT 생성
        String accessToken = jwtUtil.createAccessJwt(memberId, email, role);
        String refreshToken = jwtUtil.createRefreshJwt(memberId, email, role);

        // Redis 저장
        redisTokenRepository.deleteTokens(email);
        redisTokenRepository.save(email, accessToken, refreshToken);

        cookieUtil.addTokenCookies(response, accessToken, refreshToken);

        response.sendRedirect(REDIRECT_URI);
    }
}