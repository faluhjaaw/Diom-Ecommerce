package com.dic1.projettrans.productservice.dto;

import com.dic1.projettrans.productservice.entities.Product.SpecificationValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class SpecificationDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecDefinitionRequest {
        @NotBlank
        private String name;
        
        @NotNull
        private SpecificationValue.SpecificationType type;
        
        private String description;
        private boolean required;
        private List<String> allowedValues;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySpecRequest {
        @NotBlank
        private String subCategoryId;
        
        @NotNull
        private List<SpecDefinitionRequest> specifications;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecificationFilterRequest {
        private String subCategoryId;
        private List<SpecificationFilter> filters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecificationFilter {
        private String name;
        private String value;
        private FilterOperation operation;

        public enum FilterOperation {
            EQUALS,
            GREATER_THAN,
            LESS_THAN,
            CONTAINS,
            IN
        }
    }
}