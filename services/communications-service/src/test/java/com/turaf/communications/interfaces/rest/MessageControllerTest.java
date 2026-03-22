package com.turaf.communications.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.application.dto.MessageDTO;
import com.turaf.communications.application.dto.PaginatedMessages;
import com.turaf.communications.application.service.MessageService;
import com.turaf.communications.application.service.UnreadCountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {
    
    private MockMvc mockMvc;
    
    @Mock
    private MessageService messageService;
    
    @Mock
    private UnreadCountService unreadCountService;
    
    @InjectMocks
    private MessageController messageController;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(messageController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }
    
    @Test
    void getMessages_shouldReturnPaginatedMessages() throws Exception {
        PaginatedMessages paginatedMessages = PaginatedMessages.builder()
            .messages(List.of(new MessageDTO()))
            .page(0)
            .size(50)
            .totalElements(1)
            .totalPages(1)
            .build();
        
        when(messageService.getMessages(eq("conv-1"), eq("user1"), any(Pageable.class)))
            .thenReturn(paginatedMessages);
        
        mockMvc.perform(get("/conversations/conv-1/messages")
                .header("X-User-Id", "user1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(50));
    }
    
    @Test
    void markAsRead_shouldReturnNoContent() throws Exception {
        doNothing().when(unreadCountService)
            .markAsRead("user1", "conv-1", "msg-1");
        
        String requestBody = """
            {
                "lastMessageId": "msg-1"
            }
            """;
        
        mockMvc.perform(post("/conversations/conv-1/messages/read")
                .header("X-User-Id", "user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNoContent());
    }
    
    @Test
    void getUnreadCount_shouldReturnCount() throws Exception {
        when(unreadCountService.getUnreadCount("user1", "conv-1"))
            .thenReturn(5);
        
        mockMvc.perform(get("/conversations/conv-1/messages/unread-count")
                .header("X-User-Id", "user1"))
            .andExpect(status().isOk())
            .andExpect(content().string("5"));
    }
}
