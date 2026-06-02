package com.dic1.projettrans.productservice.dto;

import lombok.Data;

@Data
public class ShopUpdateRequest {
    private String logoUrl;
    private String bannerUrl;
    private String description;
}
