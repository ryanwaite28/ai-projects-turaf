package com.turaf.organization.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.organization.application.MembershipService;
import com.turaf.organization.application.dto.AddMemberRequest;
import com.turaf.organization.application.dto.MemberDto;
import com.turaf.organization.application.exception.MemberAlreadyExistsException;
import com.turaf.organization.application.exception.MemberNotFoundException;
import com.turaf.organization.domain.MemberRole;
import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for MembershipController.
 */
@WebMvcTest(MembershipController.class)
class MembershipControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private MembershipService membershipService;
    
    @Test
    @WithMockUser(username = "admin123")
    void shouldAddMember() throws Exception {
        // Given
        AddMemberRequest request = new AddMemberRequest("user456", "MEMBER");
        
        MemberDto responseDto = new MemberDto();
        responseDto.setId("member-123");
        responseDto.setOrganizationId("org-123");
        responseDto.setUserId("user456");
        responseDto.setRole("MEMBER");
        responseDto.setAddedBy("admin123");
        responseDto.setAddedAt(Instant.now());
        
        when(membershipService.addMember(any(OrganizationId.class), any(AddMemberRequest.class), any(UserId.class)))
            .thenReturn(responseDto);
        
        // When/Then
        mockMvc.perform(post("/api/v1/organizations/org-123/members")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value("user456"))
            .andExpect(jsonPath("$.role").value("MEMBER"));
        
        verify(membershipService).addMember(any(OrganizationId.class), any(AddMemberRequest.class), any(UserId.class));
    }
    
    @Test
    @WithMockUser(username = "admin123")
    void shouldReturnConflictWhenMemberAlreadyExists() throws Exception {
        // Given
        AddMemberRequest request = new AddMemberRequest("user456", "MEMBER");
        
        when(membershipService.addMember(any(OrganizationId.class), any(AddMemberRequest.class), any(UserId.class)))
            .thenThrow(new MemberAlreadyExistsException("User is already a member"));
        
        // When/Then
        mockMvc.perform(post("/api/v1/organizations/org-123/members")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("MEMBER_ALREADY_EXISTS"));
    }
    
    @Test
    @WithMockUser(username = "user123")
    void shouldGetMembers() throws Exception {
        // Given
        MemberDto member1 = new MemberDto();
        member1.setId("member-1");
        member1.setUserId("user1");
        member1.setRole("ADMIN");
        
        MemberDto member2 = new MemberDto();
        member2.setId("member-2");
        member2.setUserId("user2");
        member2.setRole("MEMBER");
        
        List<MemberDto> members = Arrays.asList(member1, member2);
        
        when(membershipService.getMembers(any(OrganizationId.class)))
            .thenReturn(members);
        
        // When/Then
        mockMvc.perform(get("/api/v1/organizations/org-123/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].userId").value("user1"))
            .andExpect(jsonPath("$[1].userId").value("user2"));
        
        verify(membershipService).getMembers(any(OrganizationId.class));
    }
    
    @Test
    @WithMockUser(username = "user123")
    void shouldGetSpecificMember() throws Exception {
        // Given
        MemberDto responseDto = new MemberDto();
        responseDto.setId("member-123");
        responseDto.setUserId("user456");
        responseDto.setRole("MEMBER");
        
        when(membershipService.getMember(any(OrganizationId.class), any(UserId.class)))
            .thenReturn(responseDto);
        
        // When/Then
        mockMvc.perform(get("/api/v1/organizations/org-123/members/user456"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("user456"));
        
        verify(membershipService).getMember(any(OrganizationId.class), any(UserId.class));
    }
    
    @Test
    @WithMockUser(username = "user123")
    void shouldReturnNotFoundWhenMemberDoesNotExist() throws Exception {
        // Given
        when(membershipService.getMember(any(OrganizationId.class), any(UserId.class)))
            .thenThrow(new MemberNotFoundException("Member not found"));
        
        // When/Then
        mockMvc.perform(get("/api/v1/organizations/org-123/members/nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }
    
    @Test
    @WithMockUser(username = "admin123")
    void shouldUpdateMemberRole() throws Exception {
        // Given
        MembershipController.UpdateRoleRequest request = new MembershipController.UpdateRoleRequest();
        request.setRole("ADMIN");
        
        MemberDto responseDto = new MemberDto();
        responseDto.setId("member-123");
        responseDto.setUserId("user456");
        responseDto.setRole("ADMIN");
        
        when(membershipService.updateMemberRole(any(OrganizationId.class), any(UserId.class), any(MemberRole.class)))
            .thenReturn(responseDto);
        
        // When/Then
        mockMvc.perform(put("/api/v1/organizations/org-123/members/user456/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"));
        
        verify(membershipService).updateMemberRole(any(OrganizationId.class), any(UserId.class), eq(MemberRole.ADMIN));
    }
    
    @Test
    @WithMockUser(username = "admin123")
    void shouldRemoveMember() throws Exception {
        // Given
        doNothing().when(membershipService).removeMember(
            any(OrganizationId.class),
            any(UserId.class),
            any(UserId.class)
        );
        
        // When/Then
        mockMvc.perform(delete("/api/v1/organizations/org-123/members/user456")
                .with(csrf()))
            .andExpect(status().isNoContent());
        
        verify(membershipService).removeMember(
            any(OrganizationId.class),
            any(UserId.class),
            any(UserId.class)
        );
    }
    
    @Test
    @WithMockUser(username = "admin123")
    void shouldReturnBadRequestForInvalidMemberRequest() throws Exception {
        // Given - invalid request with empty userId
        AddMemberRequest request = new AddMemberRequest("", "MEMBER");
        
        // When/Then
        mockMvc.perform(post("/api/v1/organizations/org-123/members")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
