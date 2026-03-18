package com.turaf.identity.interfaces.rest.dto;

import com.turaf.identity.application.dto.TokenResponse;
import com.turaf.identity.application.dto.UserDto;

public class LoginResponseDto {

    private UserDto user;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType;

    public LoginResponseDto() {
    }

    public LoginResponseDto(UserDto user, TokenResponse tokens) {
        this.user = user;
        this.accessToken = tokens.getAccessToken();
        this.refreshToken = tokens.getRefreshToken();
        this.expiresIn = tokens.getExpiresIn();
        this.tokenType = tokens.getTokenType();
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
