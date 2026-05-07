package com.dic1.projettrans.cartservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemAddedEvent {
    private Long userId;
    private String userEmail;   // identifiant canonique pour le recommendation-service
    private String productId;
    private Instant timestamp;
}
