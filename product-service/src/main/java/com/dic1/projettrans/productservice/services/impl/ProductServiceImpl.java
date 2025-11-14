package com.dic1.projettrans.productservice.services.impl;

import com.dic1.projettrans.productservice.dto.CreateProductDTO;
import com.dic1.projettrans.productservice.dto.ProductAllDTO;
import com.dic1.projettrans.productservice.dto.ProductDTO;
import com.dic1.projettrans.productservice.dto.UpdateProductDTO;
import com.dic1.projettrans.productservice.entities.Product;
import com.dic1.projettrans.productservice.entities.ProductCondition;
import com.dic1.projettrans.productservice.entities.SubCategory;
import com.dic1.projettrans.productservice.repositories.ProductRepository;
import com.dic1.projettrans.productservice.repositories.SubCategoryRepository;
import com.dic1.projettrans.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;

    @Override
    public ProductDTO create(CreateProductDTO dto) {
        // Validate subcategory exists
        if (dto.getSubCategoryId() != null) {
            subCategoryRepository.findById(dto.getSubCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("SubCategory not found: " + dto.getSubCategoryId()));
        }
        // Build and set fields
        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .subCategoryId(dto.getSubCategoryId())
                .vendorId(dto.getVendorId())
                .brand(dto.getBrand())
                .imageUrls(dto.getImageUrls())
                .tags(dto.getTags())
                .condition(dto.getCondition())
                .rating(dto.getRating())
                .specifications(dto.getSpecifications())
                .build();
        // Generate unique slug from name if provided
        if (product.getName() != null) {
            String base = slugify(product.getName());
            product.setSlug(ensureUniqueSlug(base, null));
        }
        Product saved = productRepository.save(product);
        return toDTO(saved);
    }

    @Override
    public Optional<ProductDTO> update(String id, UpdateProductDTO dto) {
        return productRepository.findById(id).map(existing -> {
            boolean nameChanged = false;
            if (dto.getName() != null) { existing.setName(dto.getName()); nameChanged = true; }
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
            if (dto.getPrice() != null) existing.setPrice(dto.getPrice());
            if (dto.getStock() != null) existing.setStock(dto.getStock());
            if (dto.getSubCategoryId() != null) {
                // Validate subcategory exists
                subCategoryRepository.findById(dto.getSubCategoryId())
                        .orElseThrow(() -> new IllegalArgumentException("SubCategory not found: " + dto.getSubCategoryId()));
                existing.setSubCategoryId(dto.getSubCategoryId());
            }
            if (dto.getVendorId() != null) existing.setVendorId(dto.getVendorId());
            if (dto.getBrand() != null) existing.setBrand(dto.getBrand());
            if (dto.getImageUrls() != null) existing.setImageUrls(dto.getImageUrls());
            if (dto.getTags() != null) existing.setTags(dto.getTags());
            if (dto.getCondition() != null) existing.setCondition(dto.getCondition());
            if (dto.getRating() != null) existing.setRating(dto.getRating());
            System.out.println("Existing Specifications: {}" + existing.getSpecifications());

            if (dto.getSpecifications() != null) {
                if (existing.getSpecifications() == null) {
                    System.out.println("DTO Specifications: {}" + dto.getSpecifications());
                    existing.setSpecifications(dto.getSpecifications());
                } else {
                    // fusion : on met à jour les clés existantes ou on en ajoute
                    dto.getSpecifications().forEach(existing.getSpecifications()::put);
                }
            }

            if (nameChanged && existing.getName() != null) {
                String base = slugify(existing.getName());
                existing.setSlug(ensureUniqueSlug(base, existing.getId()));
            }
            Product saved = productRepository.save(existing);
            return toDTO(saved);
        });
    }

    @Override
    public boolean delete(String id) {
        if (!productRepository.existsById(id)) return false;
        productRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<ProductDTO> getById(String id) {
        return productRepository.findById(id).map(this::toDTO);
    }

    @Override
    public List<ProductAllDTO> getAll() {
        return productRepository.findAll().stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> searchByName(String query) {
        return productRepository.findByNameContainingIgnoreCase(query).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> filterByCategory(String categoryId) {
        // find all subcategories under the category, then products by those subcategory ids
        List<String> subIds = subCategoryRepository.findByCategoryId(categoryId)
                .stream().map(SubCategory::getId).collect(Collectors.toList());
        if (subIds.isEmpty()) return List.of();
        return productRepository.findBySubCategoryIdIn(subIds).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> filterByPriceRange(BigDecimal min, BigDecimal max) {
        return productRepository.findByPriceBetween(min, max).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> filterByRating(Double minRating) {
        return productRepository.findByRatingGreaterThanEqual(minRating).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> filterByCondition(ProductCondition condition) {
        return productRepository.findByCondition(condition).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    private ProductDTO toDTO(Product product) {
        if (product == null) return null;
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .subCategoryId(product.getSubCategoryId())
                .vendorId(product.getVendorId())
                .brand(product.getBrand())
                .imageUrls(product.getImageUrls())
                .tags(product.getTags())
                .condition(product.getCondition())
                .rating(product.getRating())
                .slug(product.getSlug())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .specifications(product.getSpecifications())
                .build();
    }

    private ProductAllDTO toAllDTO(Product product) {
        if (product == null) return null;
        return ProductAllDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrls(product.getImageUrls())
                .rating(product.getRating())
                .build();
    }


    private String slugify(String input) {
        String s = input.toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("-+", "-");
        s = s.replaceAll("^-|-$", "");
        return s;
    }

    private String ensureUniqueSlug(String base, String excludeId) {
        if (base == null || base.isBlank()) return null;
        String candidate = base;
        int i = 2;
        while (true) {
            Optional<Product> existing = productRepository.findBySlug(candidate);
            if (existing.isEmpty() || (excludeId != null && excludeId.equals(existing.get().getId()))) {
                return candidate;
            }
            candidate = base + "-" + i;
            i++;
        }
    }
}
