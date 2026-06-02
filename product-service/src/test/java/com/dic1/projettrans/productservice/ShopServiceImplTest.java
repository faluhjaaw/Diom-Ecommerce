package com.dic1.projettrans.productservice;

import com.dic1.projettrans.productservice.client.CustomerServiceClient;
import com.dic1.projettrans.productservice.client.UserResponse;
import com.dic1.projettrans.productservice.dto.ShopCreateRequest;
import com.dic1.projettrans.productservice.dto.ShopResponse;
import com.dic1.projettrans.productservice.dto.ShopUpdateRequest;
import com.dic1.projettrans.productservice.entities.*;
import com.dic1.projettrans.productservice.repositories.ProductRepository;
import com.dic1.projettrans.productservice.repositories.ShopRepository;
import com.dic1.projettrans.productservice.services.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private ShopRepository shopRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerServiceClient customerServiceClient;
    @InjectMocks
    private ShopServiceImpl shopService;

    // ── createShop ────────────────────────────────────────────────────────────

    @Test
    void createShop_succeeds_whenOwnerIsShopOwner() {
        UserResponse user = new UserResponse();
        user.setEmail("owner@example.com");
        user.setSellerType("SHOP_OWNER");
        when(customerServiceClient.getUserByEmail("owner@example.com"))
                .thenReturn(Optional.of(user));
        when(shopRepository.existsByOwnerId("owner@example.com")).thenReturn(false);
        when(shopRepository.existsBySlug(any())).thenReturn(false);
        when(shopRepository.save(any())).thenAnswer(inv -> {
            Shop s = inv.getArgument(0);
            s.setId("shop-1");
            return s;
        });

        ShopCreateRequest req = new ShopCreateRequest();
        req.setName("Ma Boutique");
        req.setDescription("Super boutique");
        req.setCategory("Électronique");

        ShopResponse result = shopService.createShop("owner@example.com", req);

        assertThat(result.getId()).isEqualTo("shop-1");
        assertThat(result.getName()).isEqualTo("Ma Boutique");
        assertThat(result.getSlug()).isEqualTo("ma-boutique");
        assertThat(result.getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(result.getPlan()).isEqualTo(ShopPlan.FREE);
    }

    @Test
    void createShop_throwsForbidden_whenNotShopOwner() {
        UserResponse user = new UserResponse();
        user.setSellerType("CUSTOMER");
        when(customerServiceClient.getUserByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        ShopCreateRequest req = new ShopCreateRequest();
        req.setName("Boutique");

        assertThatThrownBy(() -> shopService.createShop("user@example.com", req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createShop_throwsForbidden_whenUserNotFound() {
        when(customerServiceClient.getUserByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.createShop("ghost@example.com", new ShopCreateRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createShop_throwsConflict_whenOwnerAlreadyHasShop() {
        UserResponse user = new UserResponse();
        user.setSellerType("SHOP_OWNER");
        when(customerServiceClient.getUserByEmail("owner@example.com"))
                .thenReturn(Optional.of(user));
        when(shopRepository.existsByOwnerId("owner@example.com")).thenReturn(true);

        ShopCreateRequest req = new ShopCreateRequest();
        req.setName("Boutique 2");

        assertThatThrownBy(() -> shopService.createShop("owner@example.com", req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // ── getBySlug ─────────────────────────────────────────────────────────────

    @Test
    void getBySlug_returnsShop_whenExists() {
        Shop shop = Shop.builder()
                .id("shop-1")
                .slug("ma-boutique")
                .name("Ma Boutique")
                .stats(new Shop.ShopStats())
                .build();
        when(shopRepository.findBySlug("ma-boutique")).thenReturn(Optional.of(shop));

        ShopResponse result = shopService.getBySlug("ma-boutique");
        assertThat(result.getId()).isEqualTo("shop-1");
        assertThat(result.getSlug()).isEqualTo("ma-boutique");
    }

    @Test
    void getBySlug_throwsNotFound_whenAbsent() {
        when(shopRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.getBySlug("ghost"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── updateShop ────────────────────────────────────────────────────────────

    @Test
    void updateShop_succeeds_forOwner() {
        Shop shop = Shop.builder()
                .id("shop-1")
                .ownerId("owner@example.com")
                .name("Boutique")
                .stats(new Shop.ShopStats())
                .build();
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));
        when(shopRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShopUpdateRequest req = new ShopUpdateRequest();
        req.setDescription("Nouvelle description");
        req.setLogoUrl("https://cdn.example.com/logo.png");

        ShopResponse result = shopService.updateShop("shop-1", "owner@example.com", req);
        assertThat(result.getDescription()).isEqualTo("Nouvelle description");
        assertThat(result.getLogoUrl()).isEqualTo("https://cdn.example.com/logo.png");
    }

    @Test
    void updateShop_throwsForbidden_whenNotOwner() {
        Shop shop = Shop.builder()
                .id("shop-1")
                .ownerId("owner@example.com")
                .build();
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> shopService.updateShop("shop-1", "other@example.com", new ShopUpdateRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void updateShop_throwsNotFound_whenShopAbsent() {
        when(shopRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.updateShop("ghost", "owner@example.com", new ShopUpdateRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── getStats ──────────────────────────────────────────────────────────────

    @Test
    void getStats_returnsStats_forOwner() {
        Shop.ShopStats stats = new Shop.ShopStats();
        stats.setTotalProducts(5);

        Shop shop = Shop.builder()
                .id("shop-1")
                .ownerId("owner@example.com")
                .stats(stats)
                .build();
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        Shop.ShopStats result = shopService.getStats("shop-1", "owner@example.com");
        assertThat(result.getTotalProducts()).isEqualTo(5);
    }

    @Test
    void getStats_throwsForbidden_whenNotOwner() {
        Shop shop = Shop.builder()
                .id("shop-1")
                .ownerId("owner@example.com")
                .build();
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> shopService.getStats("shop-1", "other@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ── incrementProductCount ─────────────────────────────────────────────────

    @Test
    void incrementProductCount_incrementsByOne() {
        Shop.ShopStats stats = new Shop.ShopStats();
        stats.setTotalProducts(3);

        Shop shop = Shop.builder()
                .id("shop-1")
                .stats(stats)
                .build();
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));
        when(shopRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        shopService.incrementProductCount("shop-1");

        assertThat(shop.getStats().getTotalProducts()).isEqualTo(4);
        verify(shopRepository).save(shop);
    }

    @Test
    void incrementProductCount_doesNothing_whenShopNotFound() {
        when(shopRepository.findById("ghost")).thenReturn(Optional.empty());

        shopService.incrementProductCount("ghost");

        verify(shopRepository, never()).save(any());
    }
}
