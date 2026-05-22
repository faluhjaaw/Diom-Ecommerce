package com.dic1.projettrans.productservice.services.impl;

import com.dic1.projettrans.productservice.dto.CreateProductDTO;
import com.dic1.projettrans.productservice.dto.ProductAllDTO;
import com.dic1.projettrans.productservice.dto.ProductDTO;
import com.dic1.projettrans.productservice.dto.UpdateProductDTO;
import com.dic1.projettrans.productservice.entities.ListingStatus;
import com.dic1.projettrans.productservice.entities.Product;
import com.dic1.projettrans.productservice.entities.ProductCondition;
import com.dic1.projettrans.productservice.entities.SubCategory;
import com.dic1.projettrans.productservice.repositories.ProductRepository;
import com.dic1.projettrans.productservice.repositories.SubCategoryRepository;
import com.dic1.projettrans.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public ProductDTO create(CreateProductDTO dto, String sellerEmail) {
        if (dto.getSubCategoryId() != null) {
            subCategoryRepository.findById(dto.getSubCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("SubCategory not found: " + dto.getSubCategoryId()));
        }
        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .subCategoryId(dto.getSubCategoryId())
                .brand(dto.getBrand())
                .imageUrls(dto.getImageUrls())
                .tags(dto.getTags())
                .condition(dto.getCondition())
                .specifications(dto.getSpecifications())
                .location(dto.getLocation())
                .negotiable(dto.isNegotiable())
                .contactPhone(dto.getContactPhone())
                .sellerEmail(sellerEmail)
                .status(ListingStatus.ACTIVE)
                .build();
        if (product.getName() != null) {
            String base = slugify(product.getName());
            product.setSlug(ensureUniqueSlug(base, null));
        }
        Product saved = productRepository.save(product);
        return toDTO(saved);
    }

    @Override
    public Optional<ProductDTO> update(String id, UpdateProductDTO dto, String callerEmail, boolean isAdmin) {
        return productRepository.findById(id).map(existing -> {
            if (!isAdmin && callerEmail != null && !callerEmail.equals(existing.getSellerEmail())) {
                throw new org.springframework.security.access.AccessDeniedException("Not the owner of this listing");
            }
            boolean nameChanged = false;
            if (dto.getName() != null) { existing.setName(dto.getName()); nameChanged = true; }
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
            if (dto.getPrice() != null) existing.setPrice(dto.getPrice());
            if (dto.getStock() != null) existing.setStock(dto.getStock());
            if (dto.getSubCategoryId() != null) {
                subCategoryRepository.findById(dto.getSubCategoryId())
                        .orElseThrow(() -> new IllegalArgumentException("SubCategory not found: " + dto.getSubCategoryId()));
                existing.setSubCategoryId(dto.getSubCategoryId());
            }
            if (dto.getBrand() != null) existing.setBrand(dto.getBrand());
            if (dto.getImageUrls() != null) existing.setImageUrls(dto.getImageUrls());
            if (dto.getTags() != null) existing.setTags(dto.getTags());
            if (dto.getCondition() != null) existing.setCondition(dto.getCondition());
            if (dto.getLocation() != null) existing.setLocation(dto.getLocation());
            if (dto.getContactPhone() != null) existing.setContactPhone(dto.getContactPhone());
            existing.setNegotiable(dto.isNegotiable());

            if (dto.getSpecifications() != null) {
                if (existing.getSpecifications() == null) {
                    existing.setSpecifications(dto.getSpecifications());
                } else {
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
    public boolean delete(String id, String callerEmail, boolean isAdmin) {
        return productRepository.findById(id).map(product -> {
            if (!isAdmin && !callerEmail.equals(product.getSellerEmail())) {
                throw new org.springframework.security.access.AccessDeniedException("Not the owner of this listing");
            }
            productRepository.deleteById(id);
            return true;
        }).orElse(false);
    }

    @Override
    public Optional<ProductDTO> getById(String id) {
        return productRepository.findById(id).map(this::toDTO);
    }

    @Override
    public List<ProductAllDTO> getAll() {
        return productRepository.findByStatus(ListingStatus.ACTIVE).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> searchByName(String query) {
        return productRepository.findByNameContainingIgnoreCaseAndStatus(query, ListingStatus.ACTIVE).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> filterByCategory(String categoryId) {
        List<String> subIds = subCategoryRepository.findByCategoryId(categoryId)
                .stream().map(SubCategory::getId).collect(Collectors.toList());
        if (subIds.isEmpty()) return List.of();
        return productRepository.findBySubCategoryIdInAndStatus(subIds, ListingStatus.ACTIVE).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> filterBySubCategory(String subCategoryId) {
        return productRepository.findBySubCategoryIdAndStatus(subCategoryId, ListingStatus.ACTIVE).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> filterByPriceRange(BigDecimal min, BigDecimal max) {
        return productRepository.findByPriceBetweenAndStatus(min, max, ListingStatus.ACTIVE).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> filterByRating(Double minRating) {
        return productRepository.findByRatingGreaterThanEqualAndStatus(minRating, ListingStatus.ACTIVE).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> filterByCondition(ProductCondition condition) {
        return productRepository.findByConditionAndStatus(condition, ListingStatus.ACTIVE).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> listByVendor(Long vendorId) {
        return List.of();
    }

    @Override
    public List<ProductAllDTO> filterByLocation(String location) {
        return productRepository.findByLocationContainingIgnoreCaseAndStatus(location, ListingStatus.ACTIVE).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductAllDTO> listBySellerEmail(String sellerEmail) {
        return productRepository.findBySellerEmailOrderByCreatedAtDesc(sellerEmail).stream().map(this::toAllDTO).collect(Collectors.toList());
    }

    @Override
    public Optional<ProductDTO> markAsSold(String id, String callerEmail, boolean isAdmin) {
        return productRepository.findById(id).map(product -> {
            if (!isAdmin && !callerEmail.equals(product.getSellerEmail())) {
                throw new org.springframework.security.access.AccessDeniedException("Not the owner of this listing");
            }
            product.setStatus(ListingStatus.SOLD);
            return toDTO(productRepository.save(product));
        });
    }

    @Override
    public Optional<ProductDTO> archiveListing(String id, String callerEmail, boolean isAdmin) {
        return productRepository.findById(id).map(product -> {
            if (!isAdmin && !callerEmail.equals(product.getSellerEmail())) {
                throw new org.springframework.security.access.AccessDeniedException("Not the owner of this listing");
            }
            product.setStatus(ListingStatus.ARCHIVED);
            return toDTO(productRepository.save(product));
        });
    }

    @Override
    public Map<String, Long> getStats() {
        long total = productRepository.count();
        long outOfStock = productRepository.findAll().stream()
                .filter(p -> p.getStock() == null || p.getStock() <= 0)
                .count();
        long lowStock = productRepository.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= 5)
                .count();
        return Map.of("total", total, "outOfStock", outOfStock, "lowStock", lowStock);
    }

    @Override
    public ProductDTO decrementStock(String id, int quantity) {
        Query query = new Query(Criteria.where("_id").is(id).and("stock").gte(quantity));
        Update update = new Update().inc("stock", -quantity);
        Product updated = mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Product.class);
        if (updated == null) {
            boolean exists = productRepository.existsById(id);
            if (!exists) throw new IllegalArgumentException("Produit introuvable : " + id);
            throw new IllegalStateException("Stock insuffisant pour le produit " + id);
        }
        return toDTO(updated);
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
                .brand(product.getBrand())
                .imageUrls(product.getImageUrls())
                .tags(product.getTags())
                .condition(product.getCondition())
                .rating(product.getRating())
                .slug(product.getSlug())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .specifications(product.getSpecifications())
                .location(product.getLocation())
                .negotiable(product.isNegotiable())
                .contactPhone(product.getContactPhone())
                .sellerEmail(product.getSellerEmail())
                .status(product.getStatus())
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
                .location(product.getLocation())
                .negotiable(product.isNegotiable())
                .condition(product.getCondition())
                .sellerEmail(product.getSellerEmail())
                .createdAt(product.getCreatedAt())
                .subCategoryId(product.getSubCategoryId())
                .slug(product.getSlug())
                .status(product.getStatus())
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
