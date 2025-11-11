package com.dic1.projettrans.cartservice.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "carts")
public class Cart {
    @Id
    private String id;

    // Owner of the cart (user id)
    @Indexed(unique = true)
    private String userId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItem {
        private String productId;
        private Integer quantity; // >= 1
        private BigDecimal unitPrice; // price at the time it was added
    }
}
