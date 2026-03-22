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
