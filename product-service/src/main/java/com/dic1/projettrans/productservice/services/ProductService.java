package com.dic1.projettrans.productservice.services;

import com.dic1.projettrans.productservice.dto.CreateProductDTO;
import com.dic1.projettrans.productservice.dto.ProductAllDTO;
import com.dic1.projettrans.productservice.dto.ProductDTO;
import com.dic1.projettrans.productservice.dto.UpdateProductDTO;
import com.dic1.projettrans.productservice.entities.ProductCondition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    ProductDTO create(CreateProductDTO dto, String sellerEmail);
    Optional<ProductDTO> update(String id, UpdateProductDTO dto, String callerEmail, boolean isAdmin);
    boolean delete(String id, String callerEmail, boolean isAdmin);
    Optional<ProductDTO> markAsSold(String id, String callerEmail, boolean isAdmin);
    Optional<ProductDTO> archiveListing(String id, String callerEmail, boolean isAdmin);
    Optional<ProductDTO> getById(String id);
    List<ProductAllDTO> getAll();
    List<ProductAllDTO> searchByName(String query);
    List<ProductAllDTO> filterByCategory(String categoryId);
    List<ProductAllDTO> filterBySubCategory(String subCategoryId);
    List<ProductAllDTO> filterByPriceRange(BigDecimal min, BigDecimal max);
    List<ProductAllDTO> filterByRating(Double minRating);
    List<ProductAllDTO> filterByCondition(ProductCondition condition);
    ProductDTO decrementStock(String id, int quantity);
    List<ProductAllDTO> listByVendor(Long vendorId);
    List<ProductAllDTO> listBySellerEmail(String sellerEmail);
    List<ProductAllDTO> filterByLocation(String location);
    java.util.Map<String, Long> getStats();
}
