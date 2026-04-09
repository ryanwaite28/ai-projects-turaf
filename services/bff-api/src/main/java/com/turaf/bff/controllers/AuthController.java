package com.turaf.bff.controllers;

import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.LoginResponseDto;
import com.turaf.bff.dto.PasswordResetConfirmRequest;
import com.turaf.bff.dto.PasswordResetRequest;
import com.turaf.bff.dto.RefreshTokenRequest;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import com.turaf.bff.security.JwtTokenValidator;
import com.turaf.bff.security.TokenBlacklistService;
import com.turaf.bff.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final IdentityServiceClient identityServiceClient;
    private final JwtTokenValidator jwtTokenValidator;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        LoginResponseDto response = identityServiceClient.login(request);
        log.info("Login successful");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    public ResponseEntity<LoginResponseDto> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.getEmail());
        LoginResponseDto response = identityServiceClient.register(request);
        log.info("Registration successful");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserContext userContext) {
        log.info("Get current user request for userId: {}", userContext.getUserId());
        UserDto user = identityServiceClient.getCurrentUser(userContext.getUserId());
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserContext userContext,
            HttpServletRequest request) {
        log.info("Logout request for userId: {}", userContext.getUserId());
        String token = jwtTokenValidator.extractToken(request.getHeader("Authorization"));
        if (token != null) {
            tokenBlacklistService.invalidate(token, jwtTokenValidator.extractExpiry(token));
        }
        identityServiceClient.logout(userContext.getUserId());
        log.info("Logout successful");
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        LoginResponseDto response = identityServiceClient.refreshToken(request);
        log.info("Token refresh successful");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());
        identityServiceClient.requestPasswordReset(request);
        log.info("Password reset request successful");
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        log.info("Password reset confirmation request");
        identityServiceClient.confirmPasswordReset(request);
        log.info("Password reset confirmation successful");
        return ResponseEntity.ok().build();
    }
}
