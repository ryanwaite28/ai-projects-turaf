# Task: Implement REST Controllers

**Service**: Communications Service  
**Type**: Interface Layer  
**Priority**: High  
**Estimated Time**: 3 hours  
**Dependencies**: 004-implement-conversation-service, 005-implement-message-service, 006-implement-unread-count-service

---

## Objective

Implement REST API controllers for conversations, messages, and read state management.

---

## Acceptance Criteria

- [x] ConversationController with all endpoints
- [x] MessageController with pagination
- [x] Proper HTTP status codes
- [x] Request validation
- [x] Error handling with @ControllerAdvice
- [x] API tests pass

---

## Implementation

**File**: `interfaces/rest/ConversationController.java`

```java
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
```

**File**: `interfaces/rest/MessageController.java`

```java
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
import java.util.Map;

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
```

**File**: `interfaces/rest/UnreadCountController.java`

```java
package com.turaf.communications.interfaces.rest;

import com.turaf.communications.application.service.UnreadCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/unread-counts")
@RequiredArgsConstructor
public class UnreadCountController {
    
    private final UnreadCountService unreadCountService;
    
    @GetMapping
    public ResponseEntity<Map<String, Integer>> getAllUnreadCounts(
        @RequestHeader("X-User-Id") String userId
    ) {
        Map<String, Integer> counts = unreadCountService.getAllUnreadCounts(userId);
        return ResponseEntity.ok(counts);
    }
}
```

**File**: `interfaces/rest/GlobalExceptionHandler.java`

```java
package com.turaf.communications.interfaces.rest;

import com.turaf.communications.domain.exception.ConversationNotFoundException;
import com.turaf.communications.domain.exception.UnauthorizedParticipantException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleConversationNotFound(ConversationNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    
    @ExceptionHandler(UnauthorizedParticipantException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedParticipantException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
    
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        return buildErrorResponse(status, message, null);
    }
    
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
        HttpStatus status, 
        String message, 
        Object details
    ) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", Instant.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        if (details != null) {
            error.put("details", details);
        }
        return ResponseEntity.status(status).body(error);
    }
}
```

**File**: `application/dto/MarkAsReadRequest.java`

```java
package com.turaf.communications.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkAsReadRequest {
    @NotNull(message = "Last message ID is required")
    private String lastMessageId;
}
```

---

## References

- **Spec**: `specs/communications-service.md` (Interface Layer)
