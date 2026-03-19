package com.turaf.bff.controllers;

import com.turaf.bff.clients.OrganizationServiceClient;
import com.turaf.bff.dto.CreateOrganizationRequest;
import com.turaf.bff.dto.MemberDto;
import com.turaf.bff.dto.OrganizationDto;
import com.turaf.bff.security.JwtAuthenticationFilter;
import com.turaf.bff.security.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(OrganizationController.class)
class OrganizationControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private OrganizationServiceClient organizationServiceClient;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    private UserContext createUserContext() {
        return UserContext.builder()
            .userId("user-123")
            .organizationId("org-123")
            .email("test@example.com")
            .name("Test User")
            .build();
    }
    
    @Test
    void testGetOrganizations_Success() {
        OrganizationDto org = OrganizationDto.builder()
            .id("org-123")
            .name("Test Org")
            .ownerId("user-123")
            .build();
        
        when(organizationServiceClient.getOrganizations(anyString()))
            .thenReturn(Flux.just(org));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/organizations")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(OrganizationDto.class)
            .hasSize(1);
    }
    
    @Test
    void testCreateOrganization_Success() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
            .name("New Org")
            .description("Test Description")
            .build();
        
        OrganizationDto org = OrganizationDto.builder()
            .id("org-456")
            .name("New Org")
            .description("Test Description")
            .build();
        
        when(organizationServiceClient.createOrganization(any(), anyString()))
            .thenReturn(Mono.just(org));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .post()
            .uri("/api/v1/organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("org-456")
            .jsonPath("$.name").isEqualTo("New Org");
    }
    
    @Test
    void testGetOrganization_Success() {
        OrganizationDto org = OrganizationDto.builder()
            .id("org-789")
            .name("Specific Org")
            .build();
        
        when(organizationServiceClient.getOrganization(anyString(), anyString()))
            .thenReturn(Mono.just(org));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/organizations/org-789")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("org-789")
            .jsonPath("$.name").isEqualTo("Specific Org");
    }
    
    @Test
    void testGetMembers_Success() {
        MemberDto member = MemberDto.builder()
            .id("member-1")
            .userId("user-1")
            .userName("John Doe")
            .role("ADMIN")
            .build();
        
        when(organizationServiceClient.getMembers(anyString(), anyString()))
            .thenReturn(Flux.just(member));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/organizations/org-123/members")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(MemberDto.class)
            .hasSize(1);
    }
}
