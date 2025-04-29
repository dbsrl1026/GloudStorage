package com.gloud.auth.service;


import com.gloud.auth.dto.MemberDto;
import com.gloud.auth.dto.TokenDto;
import com.gloud.auth.entity.Member;
import com.gloud.auth.exception.UserAlreadyExistsException;
import com.gloud.auth.repository.MemberRepository;
import com.gloud.auth.repository.RedisTokenRepository;
import com.gloud.auth.util.CookieUtil;
import com.gloud.auth.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final RedisTokenRepository redisTokenRepository;
    private final CookieUtil cookieUtil;

    @Override
    public void Register(MemberDto memberDto) {

        String encodePassword = passwordEncoder.encode(memberDto.getPassword());

        if(memberRepository.existsByEmail(memberDto.getEmail())) {
            throw new UserAlreadyExistsException("User already exists");
        }

        Member member = Member.builder()
                .email(memberDto.getEmail())
                .password(encodePassword)
                .username(memberDto.getUsername())
                .build();

        memberRepository.save(member);
    }

    @Override
    public TokenDto reissueToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new JwtException("Missing cookies");
        }

        String token = null;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }

        if (token == null) {
            throw new JwtException("Refresh token not found in cookies");
        }

        String tokenType = jwtUtil.getType(token);
        if (!"RTK".equals(tokenType)) {
            throw new JwtException("Invalid or missing token type");
        }

        String email = jwtUtil.getEmail(token);
        String storedRefreshToken = redisTokenRepository.getRefreshToken(email);

        if (storedRefreshToken == null || !storedRefreshToken.equals(token)) {
            throw new JwtException("Refresh token not found in whitelist (Redis)");
        }

        jwtUtil.validateToken(token);

        Long memberId = jwtUtil.getmemberId(token);
        String role = jwtUtil.getRole(token);

        String newAccessToken = jwtUtil.createAccessJwt(memberId, email, role);
        String newRefreshToken = jwtUtil.createRefreshJwt(memberId, email, role);

        redisTokenRepository.deleteTokens(email);
        redisTokenRepository.save(email, newAccessToken, newRefreshToken);

        cookieUtil.addTokenCookies(response, newAccessToken, newRefreshToken);

        return TokenDto.builder()
                .email(email)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }


}
