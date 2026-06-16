package com.dic1.projettrans.productservice.dto;

import com.dic1.projettrans.productservice.entities.ShopPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShopPlanUpdateRequest {
    @NotNull
    private ShopPlan plan;
}
