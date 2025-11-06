package com.dic1.projettrans.productservice.controllers;

import com.dic1.projettrans.productservice.dto.SpecificationDTO.CategorySpecRequest;
import com.dic1.projettrans.productservice.dto.SpecificationDTO.SpecificationFilterRequest;
import com.dic1.projettrans.productservice.entities.CategorySpecification;
import com.dic1.projettrans.productservice.entities.Product;
import com.dic1.projettrans.productservice.services.SpecificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/specifications")
@RequiredArgsConstructor
public class SpecificationController {

    private final SpecificationService specificationService;

    @PostMapping("/category/{subCategoryId}")
    public ResponseEntity<CategorySpecification> defineSpecifications(
            @PathVariable String subCategoryId,
            @Valid @RequestBody CategorySpecRequest request) {
        CategorySpecification savedSpec = specificationService.saveSpecificationDefinition(
            subCategoryId,
            request.getSpecifications().stream()
                .map(specDef -> Product.SpecificationDefinition.builder()
                    .name(specDef.getName())
                    .type(specDef.getType())
                    .description(specDef.getDescription())
                    .required(specDef.isRequired())
                    .allowedValues(specDef.getAllowedValues())
                    .build())
                .collect(Collectors.toList())
        );
        return ResponseEntity.ok(savedSpec);
    }

    @PostMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@Valid @RequestBody SpecificationFilterRequest request) {
        List<Product> products = specificationService.searchProductsBySpecifications(request);
        return ResponseEntity.ok(products);
    }
}