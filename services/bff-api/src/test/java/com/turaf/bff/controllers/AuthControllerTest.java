package com.turaf.bff.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.LoginResponseDto;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private IdentityServiceClient identityServiceClient;
    
    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .email("test@example.com")
            .password("password")
            .build();
        
        UserDto userDto = UserDto.builder()
            .id("user-123")
            .email("test@example.com")
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .build();
        
        LoginResponseDto response = LoginResponseDto.builder()
            .accessToken("jwt-token")
            .user(userDto)
            .build();
        
        when(identityServiceClient.login(any(LoginRequest.class)))
            .thenReturn(response);
        
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.user.id").value("user-123"))
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
            .andExpect(jsonPath("$.user.username").value("testuser"));
    }
    
    @Test
    void testRegister_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .email("new@example.com")
            .password("password123")
            .username("newuser")
            .firstName("New")
            .lastName("User")
            .organizationId("org-123")
            .build();
        
        UserDto userDto = UserDto.builder()
            .id("user-456")
            .email("new@example.com")
            .username("newuser")
            .firstName("New")
            .lastName("User")
            .build();
        
        LoginResponseDto response = LoginResponseDto.builder()
            .accessToken("jwt-token")
            .user(userDto)
            .build();
        
        when(identityServiceClient.register(any(RegisterRequest.class)))
            .thenReturn(response);
        
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").value("user-456"))
            .andExpect(jsonPath("$.user.email").value("new@example.com"));
    }
    
    @Test
    void testGetCurrentUser_Success() throws Exception {
        UserDto userDto = UserDto.builder()
            .id("user-789")
            .email("current@example.com")
            .username("currentuser")
            .firstName("Current")
            .lastName("User")
            .build();
        
        when(identityServiceClient.getCurrentUser(anyString()))
            .thenReturn(userDto);
        
        mockMvc.perform(get("/api/v1/auth/me")
            .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("user-789"))
            .andExpect(jsonPath("$.email").value("current@example.com"));
    }
    
    @Test
    void testLogout_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testLogin_ValidationError() throws Exception {
        LoginRequest invalidRequest = LoginRequest.builder()
            .email("invalid-email")
            .password("")
            .build();
        
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
}
