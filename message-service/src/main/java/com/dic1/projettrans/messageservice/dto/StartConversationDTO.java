package com.dic1.projettrans.messageservice.dto;

import lombok.Data;

@Data
public class StartConversationDTO {
    private String productId;
    private String productName;
    private String productImageUrl;
    private String sellerEmail;
    private String firstMessage;
}
