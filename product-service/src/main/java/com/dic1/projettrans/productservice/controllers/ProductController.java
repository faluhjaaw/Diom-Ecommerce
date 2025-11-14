package com.dic1.projettrans.productservice.controllers;

import com.dic1.projettrans.productservice.dto.CreateProductDTO;
import com.dic1.projettrans.productservice.dto.ProductAllDTO;
import com.dic1.projettrans.productservice.dto.ProductDTO;
import com.dic1.projettrans.productservice.dto.UpdateProductDTO;
import com.dic1.projettrans.productservice.entities.ProductCondition;
import com.dic1.projettrans.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDTO> create(@RequestBody CreateProductDTO dto) {
        ProductDTO created = productService.create(dto);
        return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(@PathVariable String id, @RequestBody UpdateProductDTO dto) {
        return productService.update(id, dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = productService.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable String id) {
        return productService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ProductAllDTO>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductAllDTO>> searchByName(@RequestParam("query") String query) {
        return ResponseEntity.ok(productService.searchByName(query));
    }

    @GetMapping("/filter/category/{categoryId}")
    public ResponseEntity<List<ProductAllDTO>> filterByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(productService.filterByCategory(categoryId));
    }

    @GetMapping("/filter/price")
    public ResponseEntity<List<ProductAllDTO>> filterByPrice(@RequestParam("min") BigDecimal min,
                                                          @RequestParam("max") BigDecimal max) {
        return ResponseEntity.ok(productService.filterByPriceRange(min, max));
    }

    @GetMapping("/filter/rating")
    public ResponseEntity<List<ProductAllDTO>> filterByRating(@RequestParam("min") Double min) {
        return ResponseEntity.ok(productService.filterByRating(min));
    }

    @GetMapping("/filter/condition")
    public ResponseEntity<List<ProductAllDTO>> filterByCondition(@RequestParam("value") ProductCondition condition) {
        return ResponseEntity.ok(productService.filterByCondition(condition));
    }
}
