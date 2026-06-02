package com.dic1.projettrans.productservice.services.impl;

import com.dic1.projettrans.productservice.client.CustomerServiceClient;
import com.dic1.projettrans.productservice.dto.ProductAllDTO;
import com.dic1.projettrans.productservice.dto.ShopCreateRequest;
import com.dic1.projettrans.productservice.dto.ShopResponse;
import com.dic1.projettrans.productservice.dto.ShopUpdateRequest;
import com.dic1.projettrans.productservice.entities.*;
import com.dic1.projettrans.productservice.repositories.ProductRepository;
import com.dic1.projettrans.productservice.repositories.ShopRepository;
import com.dic1.projettrans.productservice.services.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final CustomerServiceClient customerServiceClient;

    @Override
    public ShopResponse createShop(String ownerId, ShopCreateRequest request) {
        // Vérifier que l'utilisateur est bien SHOP_OWNER
        customerServiceClient.getUserByEmail(ownerId)
                .filter(u -> "SHOP_OWNER".equals(u.getSellerType()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Seuls les utilisateurs SHOP_OWNER peuvent créer une boutique"));

        // Une boutique par owner
        if (shopRepository.existsByOwnerId(ownerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une boutique existe déjà pour cet utilisateur");
        }

        String slug = ensureUniqueSlug(slugify(request.getName()));

        Shop shop = Shop.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .slug(slug)
                .plan(ShopPlan.FREE)
                .status(ShopStatus.PENDING)
                .stats(new Shop.ShopStats())
                .build();

        return ShopResponse.from(shopRepository.save(shop));
    }

    @Override
    public ShopResponse getBySlug(String slug) {
        return shopRepository.findBySlug(slug)
                .map(ShopResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Boutique introuvable : " + slug));
    }

    @Override
    public Page<ProductAllDTO> getShopProducts(String shopId, int page, int size) {
        return productRepository.findByShopIdAndStatus(shopId, ListingStatus.ACTIVE, PageRequest.of(page, size))
                .map(this::toAllDTO);
    }

    @Override
    public ShopResponse updateShop(String shopId, String ownerId, ShopUpdateRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Boutique introuvable"));

        if (!ownerId.equals(shop.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non propriétaire de cette boutique");
        }

        if (request.getLogoUrl() != null) shop.setLogoUrl(request.getLogoUrl());
        if (request.getBannerUrl() != null) shop.setBannerUrl(request.getBannerUrl());
        if (request.getDescription() != null) shop.setDescription(request.getDescription());

        return ShopResponse.from(shopRepository.save(shop));
    }

    @Override
    public Shop.ShopStats getStats(String shopId, String ownerId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Boutique introuvable"));

        if (!ownerId.equals(shop.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non propriétaire de cette boutique");
        }

        return shop.getStats();
    }

    @Override
    public void incrementProductCount(String shopId) {
        shopRepository.findById(shopId).ifPresent(shop -> {
            shop.getStats().setTotalProducts(shop.getStats().getTotalProducts() + 1);
            shopRepository.save(shop);
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String slugify(String input) {
        String s = input.toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("-+", "-");
        s = s.replaceAll("^-|-$", "");
        return s;
    }

    private String ensureUniqueSlug(String base) {
        String candidate = base;
        if (!shopRepository.existsBySlug(candidate)) return candidate;
        candidate = base + "-" + System.currentTimeMillis();
        return candidate;
    }

    private ProductAllDTO toAllDTO(Product product) {
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
}
