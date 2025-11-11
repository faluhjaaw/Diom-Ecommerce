package com.dic1.projettrans.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateItemQuantityDTO {
    private String productId;
    private Integer quantity;
}
