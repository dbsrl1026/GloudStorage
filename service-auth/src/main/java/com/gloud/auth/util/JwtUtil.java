package com.gloud.auth.util;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {


    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long ACCESS_EXPIRATION_TIME;
    private final long REFRESH_EXPIRATION_TIME;

    public JwtUtil(
            @Value("${spring.jwt.private-key}") String privateKeyStr,
            @Value("${spring.jwt.public-key}") String publicKeyStr,
            @Value("${spring.jwt.expiration_time.access}") long AccTokenExpTime,
            @Value("${spring.jwt.expiration_time.refresh}") long RefTokenExpTime
    ) {
        try {
            byte[] privateBytes = Base64.getDecoder().decode(privateKeyStr);
            byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA keys", e);
        }
        this.ACCESS_EXPIRATION_TIME = AccTokenExpTime * 60 * 1000L;
        this.REFRESH_EXPIRATION_TIME = RefTokenExpTime * 60 * 1000L;
    }

    public void validateToken(String token) {
        Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token);
    }


    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }


    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }
    public Long getmemberId(String token) {
        return parseClaims(token).get("memberId", Long.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }


    public String getAudience(String token) {
        return parseClaims(token).getAudience();
    }

    public String createAccessJwt(Long memberId, String email, String role) {
        Claims claims = Jwts.claims();
        claims.put("memberId", memberId);
        claims.put("role", role);
        claims.put("email", email);


        return Jwts.builder()
                .setSubject(email)
                .setIssuer("gloud-auth")
                .setAudience("access")
                .setClaims(claims)
                .setIssuedAt(new Date())                                                //기본 클레임(토큰발급시간)
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION_TIME))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String createRefreshJwt(Long memberId, String email, String role) {
        Claims claims = Jwts.claims();
        claims.put("memberId", memberId);
        claims.put("role", role);
        claims.put("email", email);

        return Jwts.builder()
                .setSubject(email)
                .setIssuer("gloud-auth")
                .setAudience("refresh")
                .setClaims(claims)
                .setIssuedAt(new Date())                                                //기본 클레임(토큰발급시간)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_TIME))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .setId(UUID.randomUUID().toString()) // jti
                .compact();
    }



}
