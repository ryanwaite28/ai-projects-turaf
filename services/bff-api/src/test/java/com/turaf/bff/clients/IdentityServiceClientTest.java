package com.turaf.bff.clients;

import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IdentityServiceClientTest {
    
    private MockWebServer mockWebServer;
    private IdentityServiceClient client;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();
        
        client = new IdentityServiceClient(webClient);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testLogin_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"user-123\",\"email\":\"test@example.com\",\"name\":\"Test User\",\"token\":\"jwt-token\"}")
            .addHeader("Content-Type", "application/json"));
        
        LoginRequest request = LoginRequest.builder()
            .email("test@example.com")
            .password("password")
            .build();
        
        UserDto user = client.login(request).block();
        
        assertNotNull(user);
        assertEquals("user-123", user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getName());
        assertEquals("jwt-token", user.getToken());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/identity/auth/login", recordedRequest.getPath());
    }
    
    @Test
    void testRegister_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"user-456\",\"email\":\"new@example.com\",\"name\":\"New User\",\"token\":\"jwt-token\"}")
            .addHeader("Content-Type", "application/json"));
        
        RegisterRequest request = RegisterRequest.builder()
            .name("New User")
            .email("new@example.com")
            .password("password123")
            .build();
        
        UserDto user = client.register(request).block();
        
        assertNotNull(user);
        assertEquals("user-456", user.getId());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("New User", user.getName());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/identity/auth/register", recordedRequest.getPath());
    }
    
    @Test
    void testGetCurrentUser_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"user-789\",\"email\":\"current@example.com\",\"name\":\"Current User\"}")
            .addHeader("Content-Type", "application/json"));
        
        UserDto user = client.getCurrentUser("test-token").block();
        
        assertNotNull(user);
        assertEquals("user-789", user.getId());
        assertEquals("current@example.com", user.getEmail());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/identity/auth/me", recordedRequest.getPath());
        assertEquals("Bearer test-token", recordedRequest.getHeader("Authorization"));
    }
    
    @Test
    void testLogout_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(204));
        
        client.logout("test-token").block();
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/identity/auth/logout", recordedRequest.getPath());
        assertEquals("Bearer test-token", recordedRequest.getHeader("Authorization"));
    }
    
    @Test
    void testLogin_ServerError() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("{\"error\":\"Internal Server Error\"}"));
        
        LoginRequest request = LoginRequest.builder()
            .email("test@example.com")
            .password("wrong")
            .build();
        
        assertThrows(Exception.class, () -> client.login(request).block());
    }
}
