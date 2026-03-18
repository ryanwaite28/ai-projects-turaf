package com.turaf.identity.interfaces.rest;

import com.turaf.identity.application.AuthenticationService;
import com.turaf.identity.application.TokenService;
import com.turaf.identity.application.dto.*;
import com.turaf.identity.domain.UserId;
import com.turaf.identity.interfaces.rest.dto.LoginResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthenticationService authenticationService;
    private final TokenService tokenService;

    public AuthController(AuthenticationService authenticationService, TokenService tokenService) {
        this.authenticationService = authenticationService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponseDto> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = authenticationService.register(request);
        TokenResponse tokens = tokenService.generateTokens(
            UserId.of(user.getId()),
            "default-org"
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new LoginResponseDto(user, tokens));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequest request) {
        UserDto user = authenticationService.login(request);
        TokenResponse tokens = tokenService.generateTokens(
            UserId.of(user.getId()),
            "default-org"
        );

        return ResponseEntity.ok(new LoginResponseDto(user, tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokens = tokenService.refreshAccessToken(request);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("X-User-Id") String userId) {
        tokenService.revokeRefreshToken(UserId.of(userId));
        return ResponseEntity.noContent().build();
    }
}
