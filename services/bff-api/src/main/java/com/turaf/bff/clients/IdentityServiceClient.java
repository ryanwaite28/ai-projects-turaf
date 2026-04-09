package com.turaf.bff.clients;

import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.LoginResponseDto;
import com.turaf.bff.dto.PasswordResetConfirmRequest;
import com.turaf.bff.dto.PasswordResetRequest;
import com.turaf.bff.dto.RefreshTokenRequest;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.GetExchange;

/**
 * Identity Service HTTP Client using Spring's declarative HTTP interface.
 */
@HttpExchange(url = "/api/v1", accept = "application/json", contentType = "application/json")
public interface IdentityServiceClient {
    
    @PostExchange("/auth/login")
    LoginResponseDto login(@RequestBody LoginRequest request);
    
    @PostExchange("/auth/register")
    LoginResponseDto register(@RequestBody RegisterRequest request);
    
    @GetExchange("/users/me")
    UserDto getCurrentUser(@RequestHeader("X-User-Id") String userId);
    
    @PostExchange("/auth/logout")
    void logout(@RequestHeader("X-User-Id") String userId);
    
    @PostExchange("/auth/refresh")
    LoginResponseDto refreshToken(@RequestBody RefreshTokenRequest request);
    
    @PostExchange("/auth/password-reset/request")
    void requestPasswordReset(@RequestBody PasswordResetRequest request);
    
    @PostExchange("/auth/password-reset/confirm")
    void confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request);
}
