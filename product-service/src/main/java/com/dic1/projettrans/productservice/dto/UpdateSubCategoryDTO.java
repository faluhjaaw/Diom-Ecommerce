package com.dic1.projettrans.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSubCategoryDTO {
    private String name;
    private String description;
    private String categoryId;
    private List<String> specificationKeys;
}
