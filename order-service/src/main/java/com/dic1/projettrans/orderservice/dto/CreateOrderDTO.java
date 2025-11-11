package com.dic1.projettrans.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderDTO {
    private String userId;
    private List<OrderItemDTO> items;
    private String shippingAddress;
    private String paymentMethod;
}
