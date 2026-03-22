package com.turaf.communications.interfaces.rest;

import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.Message;
import com.turaf.communications.domain.repository.ConversationRepository;
import com.turaf.communications.domain.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UnreadCountControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @BeforeEach
    void setUp() {
        Conversation conv1 = Conversation.createDirect("user1", "user2");
        conversationRepository.save(conv1);
        
        Message msg1 = Message.create(conv1.getId(), "user1", "Message 1");
        Message msg2 = Message.create(conv1.getId(), "user1", "Message 2");
        messageRepository.save(msg1);
        messageRepository.save(msg2);
        
        Conversation conv2 = Conversation.createGroup("Group", List.of("user2"), "user1");
        conversationRepository.save(conv2);
        
        Message msg3 = Message.create(conv2.getId(), "user1", "Group message");
        messageRepository.save(msg3);
    }
    
    @Test
    void getAllUnreadCounts_shouldReturnCountsForAllConversations() throws Exception {
        mockMvc.perform(get("/unread-counts")
                .header("X-User-Id", "user2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isMap())
            .andExpect(jsonPath("$.length()").value(2));
    }
    
    @Test
    void getAllUnreadCounts_shouldReturnEmptyForUserWithNoConversations() throws Exception {
        mockMvc.perform(get("/unread-counts")
                .header("X-User-Id", "user999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}
