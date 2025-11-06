package com.dic1.projettrans.productservice.repositories;

import com.dic1.projettrans.productservice.entities.Product;
import com.dic1.projettrans.productservice.entities.ProductCondition;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findBySubCategoryId(String subCategoryId);
    List<Product> findBySubCategoryIdIn(List<String> subCategoryIds);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

    // Slug helpers
    boolean existsBySlug(String slug);
    Optional<Product> findBySlug(String slug);

    // Vendor and tags filters
    List<Product> findByVendorId(String vendorId);
    List<Product> findByTagsIn(List<String> tags);

    // Rating and condition filters
    List<Product> findByRatingGreaterThanEqual(Double minRating);
    List<Product> findByCondition(ProductCondition condition);
}
