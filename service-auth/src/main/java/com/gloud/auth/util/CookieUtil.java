package com.gloud.auth.util;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private final int ACCESS_EXPIRATION_TIME;
    private final int REFRESH_EXPIRATION_TIME;

    public CookieUtil(
            @Value("${spring.jwt.expiration_time.access}") int AccTokenExpTime,
            @Value("${spring.jwt.expiration_time.refresh}") int RefTokenExpTime
    ) {
        this.ACCESS_EXPIRATION_TIME = AccTokenExpTime * 60;
        this.REFRESH_EXPIRATION_TIME = RefTokenExpTime * 60;
    }


    public void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(ACCESS_EXPIRATION_TIME); // 예: 15분

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(REFRESH_EXPIRATION_TIME); // 예: 7일

//        accessCookie.setSecure(true); // HTTPS 전용
//        accessCookie.setDomain("yourdomain.com"); // 프론트 주소와 동일해야 함
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
