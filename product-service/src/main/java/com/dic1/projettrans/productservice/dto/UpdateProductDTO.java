package com.dic1.projettrans.productservice.dto;

import com.dic1.projettrans.productservice.entities.ProductCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String subCategoryId;
    private String vendorId;
    private String brand;
    private List<String> imageUrls;
    private List<String> tags;
    private ProductCondition condition;
    private Double rating;
    private Map<String, String> specifications;
}
