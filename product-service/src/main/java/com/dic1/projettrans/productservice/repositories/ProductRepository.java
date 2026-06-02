package com.dic1.projettrans.productservice.repositories;

import com.dic1.projettrans.productservice.entities.ListingStatus;
import com.dic1.projettrans.productservice.entities.Product;
import com.dic1.projettrans.productservice.entities.ProductCondition;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    // Tags filters
    long countByStock(Integer stock);
    List<Product> findByTagsIn(List<String> tags);

    // Rating and condition filters
    List<Product> findByRatingGreaterThanEqual(Double minRating);
    List<Product> findByCondition(ProductCondition condition);

    // Marketplace / annonce filters
    List<Product> findBySellerEmail(String sellerEmail);
    List<Product> findByLocationContainingIgnoreCase(String location);

    // Status-filtered queries (show only active in catalogue)
    List<Product> findByStatus(ListingStatus status);
    List<Product> findBySubCategoryIdAndStatus(String subCategoryId, ListingStatus status);
    List<Product> findBySubCategoryIdInAndStatus(List<String> subCategoryIds, ListingStatus status);
    List<Product> findByNameContainingIgnoreCaseAndStatus(String name, ListingStatus status);
    List<Product> findByPriceBetweenAndStatus(BigDecimal min, BigDecimal max, ListingStatus status);
    List<Product> findByRatingGreaterThanEqualAndStatus(Double minRating, ListingStatus status);
    List<Product> findByConditionAndStatus(ProductCondition condition, ListingStatus status);
    List<Product> findByLocationContainingIgnoreCaseAndStatus(String location, ListingStatus status);
    List<Product> findBySellerEmailOrderByCreatedAtDesc(String sellerEmail);

    // Shop queries
    Page<Product> findByShopIdAndStatus(String shopId, ListingStatus status, Pageable pageable);
}
