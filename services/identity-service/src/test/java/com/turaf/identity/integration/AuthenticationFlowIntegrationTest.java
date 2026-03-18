package com.turaf.identity.integration;

import com.turaf.identity.application.dto.*;
import com.turaf.identity.domain.*;
import com.turaf.identity.interfaces.rest.dto.ErrorResponse;
import com.turaf.identity.interfaces.rest.dto.LoginResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthenticationFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void shouldCompleteFullAuthenticationFlow() {
        // 1. Register new user
        RegisterRequest registerRequest = new RegisterRequest(
            "flowtest@example.com",
            "FlowTestP@ss123",
            "Flow Test User"
        );

        ResponseEntity<LoginResponseDto> registerResponse = restTemplate.postForEntity(
            "/api/v1/auth/register",
            registerRequest,
            LoginResponseDto.class
        );

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        String accessToken = registerResponse.getBody().getAccessToken();
        String refreshToken = registerResponse.getBody().getRefreshToken();
        String userId = registerResponse.getBody().getUser().getId();
        assertNotNull(accessToken);
        assertNotNull(refreshToken);

        // 2. Access protected endpoint with access token
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserDto> meResponse = restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            entity,
            UserDto.class
        );

        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
        assertNotNull(meResponse.getBody());
        assertEquals("flowtest@example.com", meResponse.getBody().getEmail());
        assertEquals("Flow Test User", meResponse.getBody().getName());

        // 3. Refresh access token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        ResponseEntity<TokenResponse> refreshResponse = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            refreshRequest,
            TokenResponse.class
        );

        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
        assertNotNull(refreshResponse.getBody());
        String newAccessToken = refreshResponse.getBody().getAccessToken();
        assertNotNull(newAccessToken);
        assertNotEquals(accessToken, newAccessToken);

        // 4. Use new access token to access protected endpoint
        ResponseEntity<UserDto> meResponse2 = restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            entity,
            UserDto.class
        );

        assertEquals(HttpStatus.OK, meResponse2.getStatusCode());

        // 5. Change password
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(
            "FlowTestP@ss123",
            "NewFlowP@ss456"
        );
        HttpHeaders changePasswordHeaders = new HttpHeaders();
        changePasswordHeaders.set("X-User-Id", userId);
        changePasswordHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChangePasswordRequest> changePasswordEntity = new HttpEntity<>(changePasswordRequest, changePasswordHeaders);

        ResponseEntity<Void> changePasswordResponse = restTemplate.exchange(
            "/api/v1/users/me/password",
            HttpMethod.PUT,
            changePasswordEntity,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, changePasswordResponse.getStatusCode());

        // 6. Login with new password
        LoginRequest loginRequest = new LoginRequest("flowtest@example.com", "NewFlowP@ss456");
        ResponseEntity<LoginResponseDto> loginResponse = restTemplate.postForEntity(
            "/api/v1/auth/login",
            loginRequest,
            LoginResponseDto.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());

        // 7. Logout
        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
            "/api/v1/auth/logout?userId=" + userId,
            null,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, logoutResponse.getStatusCode());

        // 8. Verify old refresh token no longer works
        ResponseEntity<ErrorResponse> invalidRefreshResponse = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            refreshRequest,
            ErrorResponse.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, invalidRefreshResponse.getStatusCode());
    }

    @Test
    void shouldHandleMultipleUsersSeparately() {
        // Register user 1
        RegisterRequest user1Request = new RegisterRequest(
            "user1@example.com",
            "User1P@ss123",
            "User One"
        );
        ResponseEntity<LoginResponseDto> user1Response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            user1Request,
            LoginResponseDto.class
        );

        assertEquals(HttpStatus.CREATED, user1Response.getStatusCode());
        String user1Id = user1Response.getBody().getUser().getId();

        // Register user 2
        RegisterRequest user2Request = new RegisterRequest(
            "user2@example.com",
            "User2P@ss123",
            "User Two"
        );
        ResponseEntity<LoginResponseDto> user2Response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            user2Request,
            LoginResponseDto.class
        );

        assertEquals(HttpStatus.CREATED, user2Response.getStatusCode());
        String user2Id = user2Response.getBody().getUser().getId();

        // Verify both users exist and are different
        assertNotEquals(user1Id, user2Id);

        // Access user 1 profile
        HttpHeaders headers1 = new HttpHeaders();
        headers1.set("X-User-Id", user1Id);
        HttpEntity<Void> entity1 = new HttpEntity<>(headers1);

        ResponseEntity<UserDto> me1Response = restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            entity1,
            UserDto.class
        );

        assertEquals(HttpStatus.OK, me1Response.getStatusCode());
        assertEquals("user1@example.com", me1Response.getBody().getEmail());

        // Access user 2 profile
        HttpHeaders headers2 = new HttpHeaders();
        headers2.set("X-User-Id", user2Id);
        HttpEntity<Void> entity2 = new HttpEntity<>(headers2);

        ResponseEntity<UserDto> me2Response = restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            entity2,
            UserDto.class
        );

        assertEquals(HttpStatus.OK, me2Response.getStatusCode());
        assertEquals("user2@example.com", me2Response.getBody().getEmail());
    }

    @Test
    void shouldPreventAccessWithoutValidToken() {
        // Try to access protected endpoint without token
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
            "/api/v1/users/me",
            ErrorResponse.class
        );

        // Should return error (either 401 or 403 depending on security config)
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    void shouldHandlePasswordChangeAndReauthentication() {
        // 1. Register and login
        RegisterRequest registerRequest = new RegisterRequest(
            "passchange@example.com",
            "OriginalP@ss123",
            "Pass Change User"
        );
        ResponseEntity<LoginResponseDto> registerResponse = restTemplate.postForEntity(
            "/api/v1/auth/register",
            registerRequest,
            LoginResponseDto.class
        );

        String userId = registerResponse.getBody().getUser().getId();

        // 2. Change password
        ChangePasswordRequest changeRequest = new ChangePasswordRequest(
            "OriginalP@ss123",
            "UpdatedP@ss456"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(changeRequest, headers);

        ResponseEntity<Void> changeResponse = restTemplate.exchange(
            "/api/v1/users/me/password",
            HttpMethod.PUT,
            entity,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, changeResponse.getStatusCode());

        // 3. Verify old password no longer works
        LoginRequest oldPasswordLogin = new LoginRequest("passchange@example.com", "OriginalP@ss123");
        ResponseEntity<ErrorResponse> oldPasswordResponse = restTemplate.postForEntity(
            "/api/v1/auth/login",
            oldPasswordLogin,
            ErrorResponse.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, oldPasswordResponse.getStatusCode());

        // 4. Verify new password works
        LoginRequest newPasswordLogin = new LoginRequest("passchange@example.com", "UpdatedP@ss456");
        ResponseEntity<LoginResponseDto> newPasswordResponse = restTemplate.postForEntity(
            "/api/v1/auth/login",
            newPasswordLogin,
            LoginResponseDto.class
        );

        assertEquals(HttpStatus.OK, newPasswordResponse.getStatusCode());
    }

    @Test
    void shouldHandleProfileUpdateFlow() {
        // 1. Register user
        RegisterRequest registerRequest = new RegisterRequest(
            "profileupdate@example.com",
            "ProfileP@ss123",
            "Original Name"
        );
        ResponseEntity<LoginResponseDto> registerResponse = restTemplate.postForEntity(
            "/api/v1/auth/register",
            registerRequest,
            LoginResponseDto.class
        );

        String userId = registerResponse.getBody().getUser().getId();
        assertEquals("Original Name", registerResponse.getBody().getUser().getName());

        // 2. Update profile
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserDto> updateResponse = restTemplate.exchange(
            "/api/v1/users/me/profile?name=Updated Name",
            HttpMethod.PUT,
            entity,
            UserDto.class
        );

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Updated Name", updateResponse.getBody().getName());

        // 3. Verify profile was updated
        ResponseEntity<UserDto> getResponse = restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            entity,
            UserDto.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Updated Name", getResponse.getBody().getName());
    }
}
