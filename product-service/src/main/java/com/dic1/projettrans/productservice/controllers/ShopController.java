package com.dic1.projettrans.productservice.controllers;

import com.dic1.projettrans.productservice.dto.ProductAllDTO;
import com.dic1.projettrans.productservice.dto.ShopCreateRequest;
import com.dic1.projettrans.productservice.dto.ShopResponse;
import com.dic1.projettrans.productservice.dto.ShopUpdateRequest;
import com.dic1.projettrans.productservice.entities.Shop;
import com.dic1.projettrans.productservice.services.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    /** Créer une boutique — ownerId = email JWT */
    @PostMapping
    public ResponseEntity<ShopResponse> createShop(
            @Valid @RequestBody ShopCreateRequest request,
            Authentication authentication) {
        ShopResponse created = shopService.createShop(authentication.getName(), request);
        return ResponseEntity
                .created(URI.create("/api/shops/" + created.getSlug()))
                .body(created);
    }

    /** Vitrine publique par slug */
    @GetMapping("/{slug}")
    public ResponseEntity<ShopResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(shopService.getBySlug(slug));
    }

    /** Produits actifs d'une boutique (paginé) */
    @GetMapping("/{shopId}/products")
    public ResponseEntity<Page<ProductAllDTO>> getShopProducts(
            @PathVariable String shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(shopService.getShopProducts(shopId, page, size));
    }

    /** Modifier logo/banner/description — authentifié */
    @PutMapping("/{shopId}")
    public ResponseEntity<ShopResponse> updateShop(
            @PathVariable String shopId,
            @RequestBody ShopUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(shopService.updateShop(shopId, authentication.getName(), request));
    }

    /** Dashboard stats — authentifié + ownership check dans le service */
    @GetMapping("/{shopId}/stats")
    public ResponseEntity<Shop.ShopStats> getStats(
            @PathVariable String shopId,
            Authentication authentication) {
        return ResponseEntity.ok(shopService.getStats(shopId, authentication.getName()));
    }
}
