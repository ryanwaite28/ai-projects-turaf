package com.turaf.bff.clients;

import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.LoginResponseDto;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IdentityServiceClientTest {
    
    private MockWebServer mockWebServer;
    private IdentityServiceClient client;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        RestClient restClient = RestClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();
        
        // Create HttpExchange proxy for the interface
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();
        client = factory.createClient(IdentityServiceClient.class);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testLogin_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"accessToken\":\"jwt-token\",\"user\":{\"id\":\"user-123\",\"email\":\"test@example.com\",\"username\":\"testuser\",\"firstName\":\"Test\",\"lastName\":\"User\"}}")
            .addHeader("Content-Type", "application/json"));
        
        LoginRequest request = LoginRequest.builder()
            .email("test@example.com")
            .password("password")
            .build();
        
        LoginResponseDto response = client.login(request);
        
        assertNotNull(response);
        assertNotNull(response.getUser());
        assertEquals("user-123", response.getUser().getId());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals("jwt-token", response.getAccessToken());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/auth/login", recordedRequest.getPath());
    }
    
    @Test
    void testRegister_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"accessToken\":\"jwt-token\",\"user\":{\"id\":\"user-456\",\"email\":\"new@example.com\",\"username\":\"newuser\",\"firstName\":\"New\",\"lastName\":\"User\"}}")
            .addHeader("Content-Type", "application/json"));
        
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser")
            .firstName("New")
            .lastName("User")
            .email("new@example.com")
            .password("password123")
            .organizationId("org-123")
            .build();
        
        LoginResponseDto response = client.register(request);
        
        assertNotNull(response);
        assertNotNull(response.getUser());
        assertEquals("user-456", response.getUser().getId());
        assertEquals("new@example.com", response.getUser().getEmail());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/auth/register", recordedRequest.getPath());
    }
    
    @Test
    void testGetCurrentUser_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"user-789\",\"email\":\"current@example.com\",\"username\":\"currentuser\",\"firstName\":\"Current\",\"lastName\":\"User\"}")
            .addHeader("Content-Type", "application/json"));
        
        UserDto user = client.getCurrentUser("user-789");
        
        assertNotNull(user);
        assertEquals("user-789", user.getId());
        assertEquals("current@example.com", user.getEmail());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/api/v1/users/me", recordedRequest.getPath());
        assertEquals("user-789", recordedRequest.getHeader("X-User-Id"));
    }
    
    @Test
    void testLogout_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(204));
        
        client.logout("user-789");
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/auth/logout", recordedRequest.getPath());
        assertEquals("user-789", recordedRequest.getHeader("X-User-Id"));
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
        
        assertThrows(Exception.class, () -> client.login(request));
    }
}
