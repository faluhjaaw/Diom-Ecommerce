package com.dic1.projettrans.messageservice.controllers;

import com.dic1.projettrans.messageservice.dto.MessageDTO;
import com.dic1.projettrans.messageservice.dto.SendMessageDTO;
import com.dic1.projettrans.messageservice.services.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;

    /**
     * STOMP endpoint: /app/chat/{conversationId}
     * Broadcasts the saved message to /topic/conversation/{conversationId}
     * and notifies the recipient at /user/{email}/queue/messages
     */
    @MessageMapping("/chat/{conversationId}")
    public void handleMessage(
            @DestinationVariable String conversationId,
            @Payload SendMessageDTO dto,
            Principal principal) {

        if (principal == null || dto.getContent() == null || dto.getContent().isBlank()) {
            return;
        }

        conversationService.sendMessage(conversationId, dto.getContent(), principal.getName());
    }
}
