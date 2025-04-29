package com.gloud.auth.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.gloud.auth.dto.ErrorResponse;
import com.gloud.auth.repository.RedisTokenRepository;
import com.gloud.auth.util.ErrorResponseUtil;
import com.gloud.auth.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final RedisTokenRepository redisTokenRepository; // Redis에서 토큰 확인할 Repository
    private final ErrorResponseUtil errorResponseUtil;

    // 해당 path에 포함하는 api만 jwt 검증함
    private static final List<RequestMatcher> excludedPaths = List.of(
            new AntPathRequestMatcher("/auth/validate"),
            new AntPathRequestMatcher("/auth/me")
//            new AntPathRequestMatcher("/login/**"), ...
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean match = excludedPaths.stream().noneMatch(matcher -> matcher.matches(request));
//        System.out.println("JWT Filter Bypass: " + request.getRequestURI() + " => " + match);
        return match;
//        return excludedPaths.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String email = null;

        try {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                throw new JwtException("Missing cookies");
            }

            String token = null;
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }

            if (token == null) {
                throw new JwtException("Access token not found in cookies");
            }

            String tokenType = jwtUtil.getType(token);
            if (tokenType == null || !"ATK".equals(tokenType)) {
                throw new JwtException("Invalid or missing token type");
            }

            //  Redis 화이트리스트 확인 (존재하지 않으면 비정상 토큰)
            email = jwtUtil.getEmail(token);
            String storedAccessToken = redisTokenRepository.getAccessToken(email);

            if (storedAccessToken == null || !storedAccessToken.equals(token)) {
                throw new JwtException("Access token not found in whitelist (Redis)");
            }
            //  JWT 유효성 검증
            jwtUtil.validateToken(token);
        } catch (JwtException e) {

            log.error("JWT Filter Bypass: " + request.getRequestURI() + " => " + e.getMessage());
            ErrorResponse errorResponse = errorResponseUtil.toErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            new ObjectMapper().writeValue(response.getWriter(), errorResponse);
            return; // 체인을 더 진행하지 않음
        }



        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (userDetails == null) {
            throw new UsernameNotFoundException("User not found");
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Authentication authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }


        filterChain.doFilter(request, response);
    }
}
