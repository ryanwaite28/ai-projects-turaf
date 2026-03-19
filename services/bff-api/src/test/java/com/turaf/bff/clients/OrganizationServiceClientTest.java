package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateOrganizationRequest;
import com.turaf.bff.dto.MemberDto;
import com.turaf.bff.dto.OrganizationDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationServiceClientTest {
    
    private MockWebServer mockWebServer;
    private OrganizationServiceClient client;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();
        
        client = new OrganizationServiceClient(webClient);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testGetOrganizations_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("[{\"id\":\"org-123\",\"name\":\"Test Org\",\"ownerId\":\"user-123\"}]")
            .addHeader("Content-Type", "application/json"));
        
        List<OrganizationDto> organizations = client.getOrganizations("user-123").collectList().block();
        
        assertNotNull(organizations);
        assertEquals(1, organizations.size());
        assertEquals("org-123", organizations.get(0).getId());
        assertEquals("Test Org", organizations.get(0).getName());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/organization/organizations", recordedRequest.getPath());
        assertEquals("user-123", recordedRequest.getHeader("X-User-Id"));
    }
    
    @Test
    void testCreateOrganization_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"org-456\",\"name\":\"New Org\",\"description\":\"Test Description\"}")
            .addHeader("Content-Type", "application/json"));
        
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
            .name("New Org")
            .description("Test Description")
            .build();
        
        OrganizationDto organization = client.createOrganization(request, "user-123").block();
        
        assertNotNull(organization);
        assertEquals("org-456", organization.getId());
        assertEquals("New Org", organization.getName());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/organization/organizations", recordedRequest.getPath());
        assertEquals("user-123", recordedRequest.getHeader("X-User-Id"));
    }
    
    @Test
    void testGetOrganization_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"org-789\",\"name\":\"Specific Org\"}")
            .addHeader("Content-Type", "application/json"));
        
        OrganizationDto organization = client.getOrganization("org-789", "user-123").block();
        
        assertNotNull(organization);
        assertEquals("org-789", organization.getId());
        assertEquals("Specific Org", organization.getName());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/organization/organizations/org-789", recordedRequest.getPath());
    }
    
    @Test
    void testGetMembers_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("[{\"id\":\"member-1\",\"userId\":\"user-1\",\"userName\":\"John Doe\",\"role\":\"ADMIN\"}]")
            .addHeader("Content-Type", "application/json"));
        
        List<MemberDto> members = client.getMembers("org-123", "user-123").collectList().block();
        
        assertNotNull(members);
        assertEquals(1, members.size());
        assertEquals("member-1", members.get(0).getId());
        assertEquals("John Doe", members.get(0).getUserName());
        assertEquals("ADMIN", members.get(0).getRole());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/organization/organizations/org-123/members", recordedRequest.getPath());
    }
}
