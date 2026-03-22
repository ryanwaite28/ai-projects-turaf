package com.turaf.communications.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.application.dto.*;
import com.turaf.communications.application.service.ConversationService;
import com.turaf.communications.domain.model.ConversationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {
    
    private MockMvc mockMvc;
    
    @Mock
    private ConversationService conversationService;
    
    @InjectMocks
    private ConversationController conversationController;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(conversationController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }
    
    @Test
    void getUserConversations_shouldReturnConversations() throws Exception {
        ConversationDTO conversation = ConversationDTO.builder()
            .id("conv-1")
            .type(ConversationType.DIRECT)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        when(conversationService.getUserConversations("user1"))
            .thenReturn(List.of(conversation));
        
        mockMvc.perform(get("/conversations")
                .header("X-User-Id", "user1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("conv-1"));
    }
    
    @Test
    void getConversation_shouldReturnConversation() throws Exception {
        ConversationDTO conversation = ConversationDTO.builder()
            .id("conv-1")
            .type(ConversationType.DIRECT)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        when(conversationService.getConversation("conv-1", "user1"))
            .thenReturn(conversation);
        
        mockMvc.perform(get("/conversations/conv-1")
                .header("X-User-Id", "user1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("conv-1"));
    }
    
    @Test
    void createConversation_shouldReturnCreated() throws Exception {
        ConversationDTO conversation = ConversationDTO.builder()
            .id("conv-1")
            .type(ConversationType.DIRECT)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        when(conversationService.createConversation(any(CreateConversationRequest.class), eq("user1")))
            .thenReturn(conversation);
        
        String requestBody = """
            {
                "type": "DIRECT",
                "participantIds": ["user2"]
            }
            """;
        
        mockMvc.perform(post("/conversations")
                .header("X-User-Id", "user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("conv-1"));
    }
    
    @Test
    void addParticipant_shouldReturnCreated() throws Exception {
        doNothing().when(conversationService)
            .addParticipant(eq("conv-1"), eq("user1"), any(AddParticipantRequest.class));
        
        String requestBody = """
            {
                "userId": "user3"
            }
            """;
        
        mockMvc.perform(post("/conversations/conv-1/participants")
                .header("X-User-Id", "user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated());
    }
    
    @Test
    void removeParticipant_shouldReturnNoContent() throws Exception {
        doNothing().when(conversationService)
            .removeParticipant("conv-1", "user1", "user2");
        
        mockMvc.perform(delete("/conversations/conv-1/participants/user2")
                .header("X-User-Id", "user1"))
            .andExpect(status().isNoContent());
    }
}
