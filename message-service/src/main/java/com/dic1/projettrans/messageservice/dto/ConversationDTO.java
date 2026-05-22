package com.dic1.projettrans.messageservice.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDTO {
    private String id;
    private String productId;
    private String productName;
    private String productImageUrl;
    private String buyerEmail;
    private String sellerEmail;
    /** Email of the other participant (not the caller) */
    private String otherParticipantEmail;
    private String lastMessagePreview;
    private Instant lastMessageAt;
    /** Unread count for the caller */
    private int unreadCount;
    private Instant createdAt;
}
