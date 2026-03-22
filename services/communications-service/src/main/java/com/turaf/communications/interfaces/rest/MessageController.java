package com.turaf.communications.interfaces.rest;

import com.turaf.communications.application.dto.MarkAsReadRequest;
import com.turaf.communications.application.dto.PaginatedMessages;
import com.turaf.communications.application.service.MessageService;
import com.turaf.communications.application.service.UnreadCountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    private final UnreadCountService unreadCountService;
    
    @GetMapping
    public ResponseEntity<PaginatedMessages> getMessages(
        @PathVariable String conversationId,
        @RequestHeader("X-User-Id") String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PaginatedMessages messages = messageService.getMessages(conversationId, userId, pageable);
        return ResponseEntity.ok(messages);
    }
    
    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(
        @PathVariable String conversationId,
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody MarkAsReadRequest request
    ) {
        unreadCountService.markAsRead(userId, conversationId, request.getLastMessageId());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadCount(
        @PathVariable String conversationId,
        @RequestHeader("X-User-Id") String userId
    ) {
        int count = unreadCountService.getUnreadCount(userId, conversationId);
        return ResponseEntity.ok(count);
    }
}
