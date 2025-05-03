package com.gloud.auth.service;

import com.gloud.auth.dto.MemberDto;
import com.gloud.auth.dto.TokenDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    void Register(MemberDto memberDto);

    TokenDto reissueToken(HttpServletRequest request, HttpServletResponse response);


}
