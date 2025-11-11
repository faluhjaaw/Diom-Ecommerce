package com.dic1.projettrans.orderservice.dto;

import com.dic1.projettrans.orderservice.entities.OrderStatus;
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
public class OrderDTO {
    private String id;
    private String userId;
    private List<OrderItemDTO> items;
    private BigDecimal total;
    private OrderStatus status;
    private String shippingAddress;
    private String paymentMethod;
    private Instant createdAt;
    private Instant updatedAt;
}
