package com.dic1.projettrans.productservice.controllers;

import com.dic1.projettrans.productservice.dto.CreateProductDTO;
import com.dic1.projettrans.productservice.dto.ProductAllDTO;
import com.dic1.projettrans.productservice.dto.ProductDTO;
import com.dic1.projettrans.productservice.dto.UpdateProductDTO;
import com.dic1.projettrans.productservice.entities.ProductCondition;
import com.dic1.projettrans.productservice.kafka.ProductCatalogEvent;
import com.dic1.projettrans.productservice.kafka.ProductEventProducer;
import com.dic1.projettrans.productservice.kafka.ProductViewedEvent;
import com.dic1.projettrans.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductEventProducer productEventProducer;

    @PreAuthorize("hasAnyRole('VENDEUR', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ProductDTO> create(@RequestBody CreateProductDTO dto) {
        ProductDTO created = productService.create(dto);
        productEventProducer.publishProductCreated(toCatalogEvent("created", created));
        return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(created);
    }

    @PreAuthorize("hasAnyRole('VENDEUR', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(@PathVariable String id, @RequestBody UpdateProductDTO dto) {
        return productService.update(id, dto)
                .map(updated -> {
                    productEventProducer.publishProductUpdated(toCatalogEvent("updated", updated));
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('VENDEUR', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = productService.delete(id);
        if (deleted) productEventProducer.publishProductDeleted(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable String id) {
        return productService.getById(id)
                .map(product -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                        productEventProducer.publishProductViewed(ProductViewedEvent.builder()
                                .userEmail(auth.getName())
                                .productId(product.getId())
                                .subCategoryId(product.getSubCategoryId())
                                .timestamp(Instant.now())
                                .build());
                    }
                    return ResponseEntity.ok(product);
                })
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

    @GetMapping("/filter/subcategory/{subCategoryId}")
    public ResponseEntity<List<ProductAllDTO>> filterBySubCategory(@PathVariable String subCategoryId) {
        return ResponseEntity.ok(productService.filterBySubCategory(subCategoryId));
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

    @PatchMapping("/{id}/decrement-stock")
    public ResponseEntity<ProductDTO> decrementStock(@PathVariable String id, @RequestParam int quantity) {
        return ResponseEntity.ok(productService.decrementStock(id, quantity));
    }

    @PreAuthorize("hasAnyRole('VENDEUR', 'ADMIN')")
    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<ProductAllDTO>> listByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(productService.listByVendor(vendorId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(productService.getStats());
    }

    private ProductCatalogEvent toCatalogEvent(String eventType, ProductDTO p) {
        return ProductCatalogEvent.builder()
                .eventType(eventType)
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .subCategoryId(p.getSubCategoryId())
                .brand(p.getBrand())
                .tags(p.getTags())
                .imageUrls(p.getImageUrls())
                .slug(p.getSlug())
                .rating(p.getRating())
                .build();
    }
}
