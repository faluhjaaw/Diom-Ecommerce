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
    ProductDTO create(CreateProductDTO dto);
    Optional<ProductDTO> update(String id, UpdateProductDTO dto);
    boolean delete(String id);
    Optional<ProductDTO> getById(String id);
    List<ProductAllDTO> getAll();
    List<ProductAllDTO> searchByName(String query);
    List<ProductAllDTO> filterByCategory(String categoryId);
    List<ProductAllDTO> filterByPriceRange(BigDecimal min, BigDecimal max);
    List<ProductAllDTO> filterByRating(Double minRating);
    List<ProductAllDTO> filterByCondition(ProductCondition condition);
}
