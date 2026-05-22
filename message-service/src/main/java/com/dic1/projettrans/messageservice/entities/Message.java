package com.dic1.projettrans.messageservice.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    private String senderEmail;

    private String content;

    private Instant timestamp;

    private boolean read;
}
