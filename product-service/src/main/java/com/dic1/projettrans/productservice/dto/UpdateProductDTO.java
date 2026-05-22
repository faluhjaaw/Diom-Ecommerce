package com.dic1.projettrans.productservice.dto;

import com.dic1.projettrans.productservice.entities.ProductCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être positif")
    private BigDecimal price;
    @Min(value = 0, message = "Le stock ne peut pas être négatif")
    private Integer stock;
    private String subCategoryId;
    private String brand;
    private List<String> imageUrls;
    private List<String> tags;
    private ProductCondition condition;
    private Map<String, String> specifications;
    private String location;
    private boolean negotiable;
    private String contactPhone;
}
