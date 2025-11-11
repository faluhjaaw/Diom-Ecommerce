package com.dic1.projettrans.orderservice.dto;

import com.dic1.projettrans.orderservice.entities.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusDTO {
    private OrderStatus status;
}
