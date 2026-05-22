package com.dic1.projettrans.productservice.dto;

import com.dic1.projettrans.productservice.entities.ListingStatus;
import com.dic1.projettrans.productservice.entities.ProductCondition;
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
public class ProductAllDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Double rating;
    private List<String> imageUrls;
    private String location;
    private boolean negotiable;
    private ProductCondition condition;
    private String sellerEmail;
    private Instant createdAt;
    private String subCategoryId;
    private String slug;
    private ListingStatus status;
}
