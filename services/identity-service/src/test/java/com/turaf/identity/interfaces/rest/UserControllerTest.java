package com.turaf.identity.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.identity.application.AuthenticationService;
import com.turaf.identity.application.dto.ChangePasswordRequest;
import com.turaf.identity.application.dto.UserDto;
import com.turaf.identity.application.exception.InvalidCredentialsException;
import com.turaf.identity.application.exception.UserNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void shouldGetCurrentUser() throws Exception {
        // Given
        String userId = "user-123";
        UserDto userDto = new UserDto(userId, "test@example.com", "Test User", Instant.now(), Instant.now());

        when(authenticationService.getUserById(any(UserId.class))).thenReturn(userDto);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                .header("X-User-Id", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("Test User"));

        verify(authenticationService).getUserById(any(UserId.class));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        // Given
        String userId = "non-existent-user";
        when(authenticationService.getUserById(any(UserId.class)))
            .thenThrow(new UserNotFoundException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                .header("X-User-Id", userId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("User not found"));

        verify(authenticationService).getUserById(any(UserId.class));
    }

    @Test
    void shouldGetUserById() throws Exception {
        // Given
        String userId = "user-123";
        UserDto userDto = new UserDto(userId, "test@example.com", "Test User", Instant.now(), Instant.now());

        when(authenticationService.getUserById(any(UserId.class))).thenReturn(userDto);

        // When & Then
        mockMvc.perform(get("/api/v1/users/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("Test User"));

        verify(authenticationService).getUserById(any(UserId.class));
    }

    @Test
    void shouldChangePassword() throws Exception {
        // Given
        String userId = "user-123";
        ChangePasswordRequest request = new ChangePasswordRequest("OldP@ss123", "NewP@ss456");

        doNothing().when(authenticationService).changePassword(any(UserId.class), any(ChangePasswordRequest.class));

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/password")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(authenticationService).changePassword(any(UserId.class), any(ChangePasswordRequest.class));
    }

    @Test
    void shouldReturnUnauthorizedWhenChangePasswordWithIncorrectCurrentPassword() throws Exception {
        // Given
        String userId = "user-123";
        ChangePasswordRequest request = new ChangePasswordRequest("WrongP@ss123", "NewP@ss456");

        doThrow(new InvalidCredentialsException("Current password is incorrect"))
            .when(authenticationService).changePassword(any(UserId.class), any(ChangePasswordRequest.class));

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/password")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Current password is incorrect"));

        verify(authenticationService).changePassword(any(UserId.class), any(ChangePasswordRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenChangePasswordWithShortNewPassword() throws Exception {
        // Given
        String userId = "user-123";
        ChangePasswordRequest request = new ChangePasswordRequest("OldP@ss123", "short");

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/password")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(authenticationService, never()).changePassword(any(), any());
    }

    @Test
    void shouldReturnBadRequestWhenChangePasswordWithMissingCurrentPassword() throws Exception {
        // Given
        String userId = "user-123";
        String invalidJson = "{\"newPassword\":\"NewP@ss456\"}";

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/password")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());

        verify(authenticationService, never()).changePassword(any(), any());
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        // Given
        String userId = "user-123";
        String newName = "Updated Name";
        UserDto updatedUser = new UserDto(userId, "test@example.com", newName, Instant.now(), Instant.now());

        when(authenticationService.updateProfile(any(UserId.class), anyString())).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/profile")
                .header("X-User-Id", userId)
                .param("name", newName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.name").value(newName));

        verify(authenticationService).updateProfile(any(UserId.class), eq(newName));
    }

    @Test
    void shouldReturnNotFoundWhenUpdateProfileForNonExistentUser() throws Exception {
        // Given
        String userId = "non-existent-user";
        String newName = "Updated Name";

        when(authenticationService.updateProfile(any(UserId.class), anyString()))
            .thenThrow(new UserNotFoundException("User not found"));

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/profile")
                .header("X-User-Id", userId)
                .param("name", newName))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("User not found"));

        verify(authenticationService).updateProfile(any(UserId.class), eq(newName));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateProfileWithInvalidName() throws Exception {
        // Given
        String userId = "user-123";
        String invalidName = "";

        when(authenticationService.updateProfile(any(UserId.class), anyString()))
            .thenThrow(new IllegalArgumentException("Name cannot be blank"));

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/profile")
                .header("X-User-Id", userId)
                .param("name", invalidName))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("Name cannot be blank"));

        verify(authenticationService).updateProfile(any(UserId.class), eq(invalidName));
    }
}
