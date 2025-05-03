package com.gloud.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final Long memberId;
    private final String email;
    private final String password;
    private final String memberName;
    private final List<GrantedAuthority> authorities;

    //private MemberDTO memberDTO;                                                  //---------------------- 추가

    public CustomUserDetails(Long memberId, String email, String password, String memberName, List<GrantedAuthority> authorities) {
        this.memberId = memberId;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.memberName = memberName;
                                    //---------------------- 추가
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }


    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
    public Long getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
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
        return true;
    }


}
