package com.dic1.projettrans.messageservice.controllers;

import com.dic1.projettrans.messageservice.dto.ConversationDTO;
import com.dic1.projettrans.messageservice.dto.MessageDTO;
import com.dic1.projettrans.messageservice.dto.StartConversationDTO;
import com.dic1.projettrans.messageservice.services.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /** Start or reuse a conversation about a product */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversationDTO> startConversation(
            @RequestBody StartConversationDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(conversationService.startConversation(dto, authentication.getName()));
    }

    /** Get all conversations for the authenticated user */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationDTO>> getMyConversations(Authentication authentication) {
        return ResponseEntity.ok(conversationService.getMyConversations(authentication.getName()));
    }

    /** Get all messages for a conversation */
    @GetMapping("/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable String conversationId,
            Authentication authentication) {
        return ResponseEntity.ok(conversationService.getMessages(conversationId, authentication.getName()));
    }

    /** Mark all messages in a conversation as read */
    @PatchMapping("/{conversationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String conversationId,
            Authentication authentication) {
        conversationService.markAsRead(conversationId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /** Total unread message count across all conversations */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(Authentication authentication) {
        int count = conversationService.getTotalUnreadCount(authentication.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
