package com.turaf.communications.interfaces.rest;

import com.turaf.communications.application.dto.*;
import com.turaf.communications.application.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {
    
    private final ConversationService conversationService;
    
    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getUserConversations(
        @RequestHeader("X-User-Id") String userId
    ) {
        List<ConversationDTO> conversations = conversationService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> getConversation(
        @PathVariable String id,
        @RequestHeader("X-User-Id") String userId
    ) {
        ConversationDTO conversation = conversationService.getConversation(id, userId);
        return ResponseEntity.ok(conversation);
    }
    
    @PostMapping
    public ResponseEntity<ConversationDTO> createConversation(
        @Valid @RequestBody CreateConversationRequest request,
        @RequestHeader("X-User-Id") String userId
    ) {
        ConversationDTO conversation = conversationService.createConversation(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }
    
    @PostMapping("/{id}/participants")
    public ResponseEntity<Void> addParticipant(
        @PathVariable String id,
        @Valid @RequestBody AddParticipantRequest request,
        @RequestHeader("X-User-Id") String userId
    ) {
        conversationService.addParticipant(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @DeleteMapping("/{id}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(
        @PathVariable String id,
        @PathVariable String userId,
        @RequestHeader("X-User-Id") String requesterId
    ) {
        conversationService.removeParticipant(id, requesterId, userId);
        return ResponseEntity.noContent().build();
    }
}
