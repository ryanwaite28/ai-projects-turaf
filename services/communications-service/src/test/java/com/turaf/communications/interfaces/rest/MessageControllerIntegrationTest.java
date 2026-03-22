package com.turaf.communications.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.application.dto.MarkAsReadRequest;
import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.Message;
import com.turaf.communications.domain.repository.ConversationRepository;
import com.turaf.communications.domain.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MessageControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    private String conversationId;
    private String messageId;
    
    @BeforeEach
    void setUp() {
        Conversation conversation = Conversation.createDirect("user1", "user2");
        conversationRepository.save(conversation);
        conversationId = conversation.getId();
        
        Message msg1 = Message.create(conversationId, "user1", "Hello");
        Message msg2 = Message.create(conversationId, "user2", "Hi there");
        Message msg3 = Message.create(conversationId, "user1", "How are you?");
        
        messageRepository.save(msg1);
        messageRepository.save(msg2);
        messageRepository.save(msg3);
        messageId = msg3.getId();
    }
    
    @Test
    void getMessages_shouldReturnPaginatedMessages() throws Exception {
        mockMvc.perform(get("/conversations/" + conversationId + "/messages")
                .header("X-User-Id", "user1")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages", hasSize(3)))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(1));
    }
    
    @Test
    void getMessages_shouldSupportPagination() throws Exception {
        mockMvc.perform(get("/conversations/" + conversationId + "/messages")
                .header("X-User-Id", "user1")
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages", hasSize(2)))
            .andExpect(jsonPath("$.totalPages").value(2));
    }
    
    @Test
    void getMessages_shouldReturn403ForNonParticipant() throws Exception {
        mockMvc.perform(get("/conversations/" + conversationId + "/messages")
                .header("X-User-Id", "user999"))
            .andExpect(status().isForbidden());
    }
    
    @Test
    void markAsRead_shouldUpdateReadState() throws Exception {
        MarkAsReadRequest request = new MarkAsReadRequest();
        request.setLastMessageId(messageId);
        
        mockMvc.perform(post("/conversations/" + conversationId + "/messages/read")
                .header("X-User-Id", "user2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());
    }
    
    @Test
    void getUnreadCount_shouldReturnCount() throws Exception {
        mockMvc.perform(get("/conversations/" + conversationId + "/messages/unread-count")
                .header("X-User-Id", "user2"))
            .andExpect(status().isOk())
            .andExpect(content().string("3"));
    }
    
    @Test
    void markAsRead_shouldReturn400ForInvalidRequest() throws Exception {
        MarkAsReadRequest request = new MarkAsReadRequest();
        
        mockMvc.perform(post("/conversations/" + conversationId + "/messages/read")
                .header("X-User-Id", "user2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
