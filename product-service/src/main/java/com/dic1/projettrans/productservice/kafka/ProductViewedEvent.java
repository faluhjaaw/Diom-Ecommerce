package com.dic1.projettrans.productservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductViewedEvent {
    private String userEmail;
    private String productId;
    private String subCategoryId;
    private Instant timestamp;
}
