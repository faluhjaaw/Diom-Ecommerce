package com.dic1.projettrans.messageservice.entities;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    @Indexed
    private String productId;

    private String productName;

    private String productImageUrl;

    /** Email of the buyer (the one who initiates the conversation) */
    @Indexed
    private String buyerEmail;

    /** Email of the seller (owner of the listing) */
    @Indexed
    private String sellerEmail;

    private String lastMessagePreview;

    private Instant lastMessageAt;

    private int unreadBuyer;

    private int unreadSeller;

    @CreatedDate
    private Instant createdAt;
}
