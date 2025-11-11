package com.dic1.projettrans.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDTO {
    private String id;
    private String userId;
    private List<CartItemDTO> items;
    private BigDecimal total;
    private Instant createdAt;
    private Instant updatedAt;
}
