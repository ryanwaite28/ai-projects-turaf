package com.turaf.identity.infrastructure.security;

import com.turaf.identity.domain.UserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(tokenProvider);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWithValidToken() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String bearerToken = "Bearer " + token;
        UserId userId = UserId.generate();
        String organizationId = "org-123";

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(tokenProvider.getOrganizationIdFromToken(token)).thenReturn(organizationId);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof UserPrincipal);
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assertEquals(userId.getValue(), principal.getUserId());
        assertEquals(organizationId, principal.getOrganizationId());

        verify(tokenProvider).validateToken(token);
        verify(tokenProvider).getUserIdFromToken(token);
        verify(tokenProvider).getOrganizationIdFromToken(token);
        verify(response).setHeader("X-User-Id", userId.getValue());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWithInvalidToken() throws ServletException, IOException {
        // Given
        String token = "invalid.jwt.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(tokenProvider).validateToken(token);
        verify(tokenProvider, never()).getUserIdFromToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWithoutAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWithMalformedAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("InvalidHeader token");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWithEmptyBearerToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueFilterChainEvenWhenExceptionOccurs() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(token)).thenThrow(new RuntimeException("Token validation error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldExtractTokenFromBearerHeader() throws ServletException, IOException {
        // Given
        String token = "my.jwt.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(UserId.generate());
        when(tokenProvider.getOrganizationIdFromToken(token)).thenReturn("org-123");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider).validateToken(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSetUserIdHeaderWhenAuthenticated() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String bearerToken = "Bearer " + token;
        UserId userId = UserId.generate();

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(tokenProvider.getOrganizationIdFromToken(token)).thenReturn("org-123");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-User-Id", userId.getValue());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetUserIdHeaderWhenNotAuthenticated() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response, never()).setHeader(eq("X-User-Id"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldHandleTokenWithoutOrganizationId() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String bearerToken = "Bearer " + token;
        UserId userId = UserId.generate();

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(tokenProvider.getOrganizationIdFromToken(token)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assertEquals(userId.getValue(), principal.getUserId());
        assertNull(principal.getOrganizationId());

        verify(filterChain).doFilter(request, response);
    }
}
