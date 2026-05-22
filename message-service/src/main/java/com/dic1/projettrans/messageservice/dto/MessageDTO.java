package com.dic1.projettrans.messageservice.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
    private String id;
    private String conversationId;
    private String senderEmail;
    private String content;
    private Instant timestamp;
    private boolean read;
    private boolean mine; // true if senderEmail == caller
}
