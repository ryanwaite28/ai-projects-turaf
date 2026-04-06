package com.turaf.identity.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.identity.application.AuthenticationService;
import com.turaf.identity.application.TokenService;
import com.turaf.identity.application.dto.*;
import com.turaf.identity.application.exception.InvalidCredentialsException;
import com.turaf.identity.application.exception.InvalidTokenException;
import com.turaf.identity.application.exception.UserAlreadyExistsException;
import com.turaf.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private TokenService tokenService;

    @Test
    void shouldRegisterNewUser() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "SecureP@ss123", "testuser", "Test", "User", "org-123");
        UserDto userDto = new UserDto("user-123", "test@example.com", "testuser", "Test", "User", Instant.now(), Instant.now());
        TokenResponse tokenResponse = new TokenResponse("access.token", "refresh.token", 900);

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(userDto);
        when(tokenService.generateTokens(any(UserId.class), anyString())).thenReturn(tokenResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.id").value("user-123"))
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
            .andExpect(jsonPath("$.user.username").value("testuser"))
            .andExpect(jsonPath("$.accessToken").value("access.token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh.token"))
            .andExpect(jsonPath("$.expiresIn").value(900));

        verify(authenticationService).register(any(RegisterRequest.class));
        verify(tokenService).generateTokens(any(UserId.class), anyString());
    }

    @Test
    void shouldReturnConflictWhenRegisteringDuplicateEmail() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "SecureP@ss123", "testuser", "Test", "User", "org-123");
        when(authenticationService.register(any(RegisterRequest.class)))
            .thenThrow(new UserAlreadyExistsException("User with email test@example.com already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
            .andExpect(jsonPath("$.message").value("User with email test@example.com already exists"));

        verify(authenticationService).register(any(RegisterRequest.class));
        verify(tokenService, never()).generateTokens(any(), anyString());
    }

    @Test
    void shouldReturnBadRequestWhenRegisteringWithInvalidEmail() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("invalid-email", "SecureP@ss123", "testuser", "Test", "User", "org-123");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(authenticationService, never()).register(any());
    }

    @Test
    void shouldReturnBadRequestWhenRegisteringWithShortPassword() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "short", "testuser", "Test", "User", "org-123");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(authenticationService, never()).register(any());
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "SecureP@ss123");
        UserDto userDto = new UserDto("user-123", "test@example.com", "testuser", "Test", "User", Instant.now(), Instant.now());
        TokenResponse tokenResponse = new TokenResponse("access.token", "refresh.token", 900);

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(userDto);
        when(authenticationService.getUserOrganizationId(any(UserId.class))).thenReturn("org-123");
        when(tokenService.generateTokens(any(UserId.class), anyString())).thenReturn(tokenResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").value("user-123"))
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
            .andExpect(jsonPath("$.accessToken").value("access.token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh.token"));

        verify(authenticationService).login(any(LoginRequest.class));
        verify(tokenService).generateTokens(any(UserId.class), anyString());
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginWithInvalidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "WrongPassword");
        when(authenticationService.login(any(LoginRequest.class)))
            .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(authenticationService).login(any(LoginRequest.class));
        verify(tokenService, never()).generateTokens(any(), anyString());
    }

    @Test
    void shouldRefreshAccessToken() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-uuid");
        TokenResponse tokenResponse = new TokenResponse("new.access.token", "refresh-token-uuid", 900);

        when(tokenService.refreshAccessToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new.access.token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-uuid"))
            .andExpect(jsonPath("$.expiresIn").value(900));

        verify(tokenService).refreshAccessToken(any(RefreshTokenRequest.class));
    }

    @Test
    void shouldReturnUnauthorizedWhenRefreshingWithInvalidToken() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
        when(tokenService.refreshAccessToken(any(RefreshTokenRequest.class)))
            .thenThrow(new InvalidTokenException("Invalid refresh token"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
            .andExpect(jsonPath("$.message").value("Invalid refresh token"));

        verify(tokenService).refreshAccessToken(any(RefreshTokenRequest.class));
    }

    @Test
    void shouldReturnUnauthorizedWhenRefreshingWithExpiredToken() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("expired-token");
        when(tokenService.refreshAccessToken(any(RefreshTokenRequest.class)))
            .thenThrow(new InvalidTokenException("Refresh token expired"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
            .andExpect(jsonPath("$.message").value("Refresh token expired"));

        verify(tokenService).refreshAccessToken(any(RefreshTokenRequest.class));
    }

    @Test
    void shouldLogoutUser() throws Exception {
        // Given
        String userId = "user-123";

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("X-User-Id", userId))
            .andExpect(status().isNoContent());

        verify(tokenService).revokeRefreshToken(any(UserId.class));
    }

    @Test
    void shouldReturnBadRequestWhenRegisteringWithMissingFields() throws Exception {
        // Given
        String invalidJson = "{\"email\":\"test@example.com\"}";

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void shouldReturnBadRequestWhenLoginWithMissingPassword() throws Exception {
        // Given
        String invalidJson = "{\"email\":\"test@example.com\"}";

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    @Test
    void shouldReturnBadRequestWhenRefreshWithBlankToken() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(tokenService, never()).refreshAccessToken(any());
    }

    @Test
    void shouldRequestPasswordReset() throws Exception {
        // Given
        PasswordResetRequest request = new PasswordResetRequest("test@example.com");
        doNothing().when(authenticationService).requestPasswordReset(any(PasswordResetRequest.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(authenticationService).requestPasswordReset(any(PasswordResetRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenRequestingPasswordResetWithInvalidEmail() throws Exception {
        // Given
        PasswordResetRequest request = new PasswordResetRequest("invalid-email");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(authenticationService, never()).requestPasswordReset(any());
    }

    @Test
    void shouldConfirmPasswordReset() throws Exception {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("valid-token", "NewP@ssw0rd!");
        doNothing().when(authenticationService).confirmPasswordReset(any(PasswordResetConfirmRequest.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(authenticationService).confirmPasswordReset(any(PasswordResetConfirmRequest.class));
    }

    @Test
    void shouldReturnUnauthorizedWhenConfirmingWithInvalidToken() throws Exception {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("invalid-token", "NewP@ssw0rd!");
        doThrow(new InvalidTokenException("Invalid password reset token"))
            .when(authenticationService).confirmPasswordReset(any(PasswordResetConfirmRequest.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        verify(authenticationService).confirmPasswordReset(any(PasswordResetConfirmRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenConfirmingWithBlankToken() throws Exception {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("", "NewP@ssw0rd!");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(authenticationService, never()).confirmPasswordReset(any());
    }

    @Test
    void shouldReturnBadRequestWhenConfirmingWithShortPassword() throws Exception {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("valid-token", "short");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(authenticationService, never()).confirmPasswordReset(any());
    }
}
