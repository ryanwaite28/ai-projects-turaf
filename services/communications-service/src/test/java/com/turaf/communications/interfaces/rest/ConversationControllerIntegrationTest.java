package com.turaf.communications.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.application.dto.AddParticipantRequest;
import com.turaf.communications.application.dto.CreateConversationRequest;
import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.ConversationType;
import com.turaf.communications.domain.model.ParticipantRole;
import com.turaf.communications.domain.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConversationControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @BeforeEach
    void setUp() {
        // Clean state handled by @Transactional rollback
    }
    
    @Test
    void createConversation_shouldCreateDirectConversation() throws Exception {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setType(ConversationType.DIRECT);
        request.setParticipantIds(List.of("user2"));
        
        mockMvc.perform(post("/conversations")
                .header("X-User-Id", "user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.type").value("DIRECT"))
            .andExpect(jsonPath("$.participants", hasSize(2)));
    }
    
    @Test
    void createConversation_shouldCreateGroupConversation() throws Exception {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setType(ConversationType.GROUP);
        request.setName("Test Group");
        request.setParticipantIds(List.of("user2", "user3"));
        
        mockMvc.perform(post("/conversations")
                .header("X-User-Id", "user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("GROUP"))
            .andExpect(jsonPath("$.name").value("Test Group"))
            .andExpect(jsonPath("$.participants", hasSize(3)));
    }
    
    @Test
    void getUserConversations_shouldReturnUserConversations() throws Exception {
        Conversation conv1 = Conversation.createDirect("user1", "user2");
        Conversation conv2 = Conversation.createGroup("Group", List.of("user2"), "user1");
        conversationRepository.save(conv1);
        conversationRepository.save(conv2);
        
        mockMvc.perform(get("/conversations")
                .header("X-User-Id", "user1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].type", containsInAnyOrder("DIRECT", "GROUP")));
    }
    
    @Test
    void getConversation_shouldReturnConversationDetails() throws Exception {
        Conversation conversation = Conversation.createDirect("user1", "user2");
        conversationRepository.save(conversation);
        
        mockMvc.perform(get("/conversations/" + conversation.getId())
                .header("X-User-Id", "user1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(conversation.getId()))
            .andExpect(jsonPath("$.type").value("DIRECT"))
            .andExpect(jsonPath("$.participants", hasSize(2)));
    }
    
    @Test
    void getConversation_shouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/conversations/non-existent-id")
                .header("X-User-Id", "user1"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getConversation_shouldReturn403ForNonParticipant() throws Exception {
        Conversation conversation = Conversation.createDirect("user2", "user3");
        conversationRepository.save(conversation);
        
        mockMvc.perform(get("/conversations/" + conversation.getId())
                .header("X-User-Id", "user1"))
            .andExpect(status().isForbidden());
    }
    
    @Test
    void addParticipant_shouldAddParticipantToGroup() throws Exception {
        Conversation conversation = Conversation.createGroup("Group", List.of("user2"), "user1");
        conversationRepository.save(conversation);
        
        AddParticipantRequest request = new AddParticipantRequest();
        request.setUserId("user3");
        request.setRole(ParticipantRole.MEMBER);
        
        mockMvc.perform(post("/conversations/" + conversation.getId() + "/participants")
                .header("X-User-Id", "user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
    
    @Test
    void removeParticipant_shouldRemoveParticipant() throws Exception {
        Conversation conversation = Conversation.createGroup("Group", List.of("user2", "user3"), "user1");
        conversationRepository.save(conversation);
        
        mockMvc.perform(delete("/conversations/" + conversation.getId() + "/participants/user3")
                .header("X-User-Id", "user1"))
            .andExpect(status().isNoContent());
    }
    
    @Test
    void createConversation_shouldReturn400ForInvalidRequest() throws Exception {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setType(ConversationType.DIRECT);
        
        mockMvc.perform(post("/conversations")
                .header("X-User-Id", "user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
