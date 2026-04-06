package com.turaf.bff.controllers;

import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthController.class)
class AuthControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private IdentityServiceClient identityServiceClient;
    
    @Test
    void testLogin_Success() {
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
        
        when(identityServiceClient.login(any(LoginRequest.class)))
            .thenReturn(Mono.just(userDto));
        
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("user-123")
            .jsonPath("$.email").isEqualTo("test@example.com")
            .jsonPath("$.username").isEqualTo("testuser");
    }
    
    @Test
    void testRegister_Success() {
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
        
        when(identityServiceClient.register(any(RegisterRequest.class)))
            .thenReturn(Mono.just(userDto));
        
        webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("user-456")
            .jsonPath("$.email").isEqualTo("new@example.com");
    }
    
    @Test
    void testGetCurrentUser_Success() {
        UserDto userDto = UserDto.builder()
            .id("user-789")
            .email("current@example.com")
            .username("currentuser")
            .firstName("Current")
            .lastName("User")
            .build();
        
        when(identityServiceClient.getCurrentUser(anyString()))
            .thenReturn(Mono.just(userDto));
        
        webTestClient.get()
            .uri("/api/v1/auth/me")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("user-789")
            .jsonPath("$.email").isEqualTo("current@example.com");
    }
    
    @Test
    void testLogout_Success() {
        when(identityServiceClient.logout(anyString()))
            .thenReturn(Mono.empty());
        
        webTestClient.post()
            .uri("/api/v1/auth/logout")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk();
    }
    
    @Test
    void testLogin_ValidationError() {
        LoginRequest invalidRequest = LoginRequest.builder()
            .email("invalid-email")
            .password("")
            .build();
        
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }
}
