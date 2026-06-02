package com.dic1.projettrans.productservice.services;

import com.dic1.projettrans.productservice.dto.ProductAllDTO;
import com.dic1.projettrans.productservice.dto.ShopCreateRequest;
import com.dic1.projettrans.productservice.dto.ShopResponse;
import com.dic1.projettrans.productservice.dto.ShopUpdateRequest;
import com.dic1.projettrans.productservice.entities.Shop;
import org.springframework.data.domain.Page;

public interface ShopService {
    ShopResponse createShop(String ownerId, ShopCreateRequest request);
    ShopResponse getBySlug(String slug);
    Page<ProductAllDTO> getShopProducts(String shopId, int page, int size);
    ShopResponse updateShop(String shopId, String ownerId, ShopUpdateRequest request);
    Shop.ShopStats getStats(String shopId, String ownerId);
    void incrementProductCount(String shopId);
}
