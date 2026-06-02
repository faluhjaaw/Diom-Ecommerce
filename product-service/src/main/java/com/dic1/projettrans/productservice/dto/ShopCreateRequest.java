package com.dic1.projettrans.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopCreateRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String category;
}
