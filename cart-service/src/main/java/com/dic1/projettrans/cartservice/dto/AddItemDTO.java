package com.dic1.projettrans.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddItemDTO {
    private String productId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
