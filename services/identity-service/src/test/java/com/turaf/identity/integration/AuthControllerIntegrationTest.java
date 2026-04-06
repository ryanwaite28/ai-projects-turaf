package com.turaf.identity.integration;

import com.turaf.identity.application.dto.*;
import com.turaf.identity.domain.*;
import com.turaf.identity.interfaces.rest.dto.ErrorResponse;
import com.turaf.identity.interfaces.rest.dto.LoginResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanup() {
        refreshTokenRepository.deleteExpiredTokens();
    }

    @Test
    void shouldRegisterNewUserWithValidData() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser@example.com",
            "SecureP@ss123",
            "newuser",
            "New",
            "User",
            "org-123"
        );

        // When
        ResponseEntity<LoginResponseDto> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            LoginResponseDto.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getAccessToken());
        assertNotNull(response.getBody().getRefreshToken());
        assertEquals("newuser@example.com", response.getBody().getUser().getEmail());
        assertEquals("newuser", response.getBody().getUser().getUsername());
        assertNotNull(response.getBody().getUser().getId());

        // Verify user saved in database
        Optional<User> savedUser = userRepository.findByEmail(new Email("newuser@example.com"));
        assertTrue(savedUser.isPresent());
        assertEquals("newuser", savedUser.get().getUsername());
    }

    @Test
    void shouldReturnConflictWhenRegisteringDuplicateEmail() {
        // Given - Create existing user
        createTestUser("existing@example.com", "ExistingP@ss123");

        RegisterRequest request = new RegisterRequest(
            "existing@example.com",
            "SecureP@ss123",
            "dupuser",
            "Duplicate",
            "User",
            "org-123"
        );

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("USER_ALREADY_EXISTS", response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("existing@example.com"));
    }

    @Test
    void shouldReturnBadRequestWhenRegisteringWithInvalidEmail() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "invalid-email",
            "SecureP@ss123",
            "testuser",
            "Test",
            "User",
            "org-123"
        );

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getCode());
    }

    @Test
    void shouldReturnBadRequestWhenRegisteringWithWeakPassword() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "weak",
            "testuser",
            "Test",
            "User",
            "org-123"
        );

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldLoginWithValidCredentials() {
        // Given
        String email = "user@example.com";
        String password = "SecureP@ss123";
        createTestUser(email, password);

        LoginRequest request = new LoginRequest(email, password);

        // When
        ResponseEntity<LoginResponseDto> response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            request,
            LoginResponseDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getAccessToken());
        assertNotNull(response.getBody().getRefreshToken());
        assertEquals(email, response.getBody().getUser().getEmail());
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginWithInvalidPassword() {
        // Given
        String email = "user@example.com";
        createTestUser(email, "CorrectP@ss123");

        LoginRequest request = new LoginRequest(email, "WrongP@ss456");

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            request,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_CREDENTIALS", response.getBody().getCode());
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginWithNonExistentEmail() {
        // Given
        LoginRequest request = new LoginRequest("nonexistent@example.com", "SomeP@ss123");

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            request,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_CREDENTIALS", response.getBody().getCode());
    }

    @Test
    void shouldRefreshAccessTokenWithValidRefreshToken() {
        // Given
        LoginResponseDto loginResponse = loginTestUser("refresh@example.com", "RefreshP@ss123");
        RefreshTokenRequest request = new RefreshTokenRequest(loginResponse.getRefreshToken());

        // When
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            request,
            TokenResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getAccessToken());
        assertEquals(loginResponse.getRefreshToken(), response.getBody().getRefreshToken());
    }

    @Test
    void shouldReturnUnauthorizedWhenRefreshingWithInvalidToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            request,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_TOKEN", response.getBody().getCode());
    }

    @Test
    void shouldReturnBadRequestWhenRefreshingWithBlankToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("");

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            request,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getCode());
    }

    @Test
    void shouldLogoutUser() {
        // Given
        LoginResponseDto loginResponse = loginTestUser("logout@example.com", "LogoutP@ss123");
        String userId = loginResponse.getUser().getId();

        // When
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/api/v1/auth/logout?userId=" + userId,
            null,
            Void.class
        );

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify refresh token no longer works
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginResponse.getRefreshToken());
        ResponseEntity<ErrorResponse> refreshResponse = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            refreshRequest,
            ErrorResponse.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, refreshResponse.getStatusCode());
    }

    // Helper methods
    private void createTestUser(String email, String password) {
        UserId userId = UserId.generate();
        Email emailObj = new Email(email);
        Password passwordObj = Password.fromRaw(password, passwordEncoder);
        String username = email.replace("@", "_").replace(".", "_");
        User user = new User(userId, "org-123", emailObj, passwordObj, username, "Test", "User");
        userRepository.save(user);
    }

    private LoginResponseDto loginTestUser(String email, String password) {
        createTestUser(email, password);
        LoginRequest request = new LoginRequest(email, password);
        ResponseEntity<LoginResponseDto> response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            request,
            LoginResponseDto.class
        );
        return response.getBody();
    }
}
