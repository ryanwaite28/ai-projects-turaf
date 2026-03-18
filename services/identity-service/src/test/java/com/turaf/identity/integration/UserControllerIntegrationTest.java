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
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldGetCurrentUserWithValidToken() {
        // Given
        LoginResponseDto loginResponse = loginTestUser("currentuser@example.com", "CurrentP@ss123");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", loginResponse.getUser().getId());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<UserDto> response = restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            entity,
            UserDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("currentuser@example.com", response.getBody().getEmail());
        assertEquals(loginResponse.getUser().getId(), response.getBody().getId());
    }

    @Test
    void shouldGetUserById() {
        // Given
        LoginResponseDto loginResponse = loginTestUser("getbyid@example.com", "GetByIdP@ss123");
        String userId = loginResponse.getUser().getId();

        // When
        ResponseEntity<UserDto> response = restTemplate.getForEntity(
            "/api/v1/users/" + userId,
            UserDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().getId());
        assertEquals("getbyid@example.com", response.getBody().getEmail());
    }

    @Test
    void shouldReturnNotFoundWhenGettingNonExistentUser() {
        // Given
        String nonExistentUserId = UserId.generate().getValue();

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
            "/api/v1/users/" + nonExistentUserId,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("USER_NOT_FOUND", response.getBody().getCode());
    }

    @Test
    void shouldChangePasswordWithCorrectCurrentPassword() {
        // Given
        String email = "changepass@example.com";
        String oldPassword = "OldP@ss123";
        String newPassword = "NewP@ss456";
        
        LoginResponseDto loginResponse = loginTestUser(email, oldPassword);
        String userId = loginResponse.getUser().getId();

        ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/v1/users/me/password",
            HttpMethod.PUT,
            entity,
            Void.class
        );

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify can login with new password
        LoginRequest loginRequest = new LoginRequest(email, newPassword);
        ResponseEntity<LoginResponseDto> loginResponse2 = restTemplate.postForEntity(
            "/api/v1/auth/login",
            loginRequest,
            LoginResponseDto.class
        );

        assertEquals(HttpStatus.OK, loginResponse2.getStatusCode());
    }

    @Test
    void shouldReturnUnauthorizedWhenChangingPasswordWithIncorrectCurrentPassword() {
        // Given
        String email = "wrongpass@example.com";
        LoginResponseDto loginResponse = loginTestUser(email, "CorrectP@ss123");
        String userId = loginResponse.getUser().getId();

        ChangePasswordRequest request = new ChangePasswordRequest("WrongP@ss456", "NewP@ss789");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/users/me/password",
            HttpMethod.PUT,
            entity,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_CREDENTIALS", response.getBody().getCode());
    }

    @Test
    void shouldReturnBadRequestWhenChangingPasswordWithWeakNewPassword() {
        // Given
        LoginResponseDto loginResponse = loginTestUser("weaknew@example.com", "CurrentP@ss123");
        String userId = loginResponse.getUser().getId();

        ChangePasswordRequest request = new ChangePasswordRequest("CurrentP@ss123", "weak");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/users/me/password",
            HttpMethod.PUT,
            entity,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldUpdateUserProfile() {
        // Given
        LoginResponseDto loginResponse = loginTestUser("updateprofile@example.com", "UpdateP@ss123");
        String userId = loginResponse.getUser().getId();
        String newName = "Updated Name";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<UserDto> response = restTemplate.exchange(
            "/api/v1/users/me/profile?name=" + newName,
            HttpMethod.PUT,
            entity,
            UserDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(newName, response.getBody().getName());
        assertEquals(userId, response.getBody().getId());
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingProfileWithInvalidName() {
        // Given
        LoginResponseDto loginResponse = loginTestUser("invalidname@example.com", "InvalidP@ss123");
        String userId = loginResponse.getUser().getId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/users/me/profile?name=",
            HttpMethod.PUT,
            entity,
            ErrorResponse.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_INPUT", response.getBody().getCode());
    }

    // Helper methods
    private void createTestUser(String email, String password) {
        UserId userId = UserId.generate();
        Email emailObj = new Email(email);
        Password passwordObj = Password.fromRaw(password, passwordEncoder);
        User user = new User(userId, emailObj, passwordObj, "Test User");
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
