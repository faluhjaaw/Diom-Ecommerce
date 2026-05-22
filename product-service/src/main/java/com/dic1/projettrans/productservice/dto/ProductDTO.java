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
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String subCategoryId;
    private String brand;
    private List<String> imageUrls;
    private List<String> tags;
    private ProductCondition condition;
    private Double rating;
    private Map<String, String> specifications;
    private String slug;
    private Instant createdAt;
    private Instant updatedAt;
    private String location;
    private boolean negotiable;
    private String contactPhone;
    private String sellerEmail;
    private ListingStatus status;
}
