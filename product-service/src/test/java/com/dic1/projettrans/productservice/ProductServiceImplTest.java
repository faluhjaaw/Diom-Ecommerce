package com.dic1.projettrans.productservice;

import com.dic1.projettrans.productservice.dto.CreateProductDTO;
import com.dic1.projettrans.productservice.dto.ProductDTO;
import com.dic1.projettrans.productservice.dto.UpdateProductDTO;
import com.dic1.projettrans.productservice.entities.ListingStatus;
import com.dic1.projettrans.productservice.entities.Product;
import com.dic1.projettrans.productservice.entities.ProductCondition;
import com.dic1.projettrans.productservice.repositories.ProductRepository;
import com.dic1.projettrans.productservice.repositories.SubCategoryRepository;
import com.dic1.projettrans.productservice.services.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private SubCategoryRepository subCategoryRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @InjectMocks
    private ProductServiceImpl productService;

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_savesProduct_andReturnsDto() {
        CreateProductDTO dto = CreateProductDTO.builder()
                .name("iPhone 14")
                .description("Bon état")
                .price(BigDecimal.valueOf(450000))
                .stock(1)
                .condition(ProductCondition.USED)
                .build();

        Product saved = Product.builder()
                .id("prod-1")
                .name("iPhone 14")
                .description("Bon état")
                .price(BigDecimal.valueOf(450000))
                .stock(1)
                .sellerEmail("seller@example.com")
                .status(ListingStatus.ACTIVE)
                .slug("iphone-14")
                .build();

        when(productRepository.findBySlug("iphone-14")).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenReturn(saved);

        ProductDTO result = productService.create(dto, "seller@example.com");

        assertThat(result.getId()).isEqualTo("prod-1");
        assertThat(result.getName()).isEqualTo("iPhone 14");
        assertThat(result.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(result.getSellerEmail()).isEqualTo("seller@example.com");
    }

    @Test
    void create_throwsException_whenSubCategoryNotFound() {
        CreateProductDTO dto = CreateProductDTO.builder()
                .name("Test")
                .description("Desc")
                .price(BigDecimal.ONE)
                .stock(1)
                .subCategoryId("invalid-cat")
                .build();

        when(subCategoryRepository.findById("invalid-cat")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(dto, "seller@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SubCategory not found");
    }

    @Test
    void create_generatesUniqueSlug_whenSlugCollides() {
        CreateProductDTO dto = CreateProductDTO.builder()
                .name("Samsung S24")
                .description("Neuf")
                .price(BigDecimal.valueOf(300000))
                .stock(1)
                .build();

        // First slug "samsung-s24" exists, "samsung-s24-2" is free
        Product conflict = Product.builder().id("other-id").slug("samsung-s24").build();
        when(productRepository.findBySlug("samsung-s24")).thenReturn(Optional.of(conflict));
        when(productRepository.findBySlug("samsung-s24-2")).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId("prod-2");
            return p;
        });

        ProductDTO result = productService.create(dto, "seller@example.com");
        assertThat(result.getSlug()).isEqualTo("samsung-s24-2");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_succeeds_forOwner() {
        Product existing = Product.builder()
                .id("prod-1")
                .name("Old Name")
                .sellerEmail("owner@example.com")
                .status(ListingStatus.ACTIVE)
                .build();

        UpdateProductDTO dto = new UpdateProductDTO();
        dto.setDescription("New description");

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<ProductDTO> result = productService.update("prod-1", dto, "owner@example.com", false);

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("New description");
    }

    @Test
    void update_throwsAccessDenied_whenNotOwnerNorAdmin() {
        Product existing = Product.builder()
                .id("prod-1")
                .sellerEmail("owner@example.com")
                .build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.update("prod-1", new UpdateProductDTO(), "other@example.com", false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_succeeds_forAdmin_evenIfNotOwner() {
        Product existing = Product.builder()
                .id("prod-1")
                .sellerEmail("owner@example.com")
                .status(ListingStatus.ACTIVE)
                .build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<ProductDTO> result = productService.update("prod-1", new UpdateProductDTO(), "admin@example.com", true);
        assertThat(result).isPresent();
    }

    @Test
    void update_returnsEmpty_whenProductNotFound() {
        when(productRepository.findById("ghost")).thenReturn(Optional.empty());
        Optional<ProductDTO> result = productService.update("ghost", new UpdateProductDTO(), "any@example.com", true);
        assertThat(result).isEmpty();
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_returnsTrue_forOwner() {
        Product product = Product.builder()
                .id("prod-1")
                .sellerEmail("owner@example.com")
                .build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThat(productService.delete("prod-1", "owner@example.com", false)).isTrue();
        verify(productRepository).deleteById("prod-1");
    }

    @Test
    void delete_returnsFalse_whenNotFound() {
        when(productRepository.findById("ghost")).thenReturn(Optional.empty());
        assertThat(productService.delete("ghost", "any@example.com", true)).isFalse();
    }

    @Test
    void delete_throwsAccessDenied_whenNotOwnerNorAdmin() {
        Product product = Product.builder()
                .id("prod-1")
                .sellerEmail("owner@example.com")
                .build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.delete("prod-1", "thief@example.com", false))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsOnlyActiveProducts() {
        Product active = Product.builder().id("p1").status(ListingStatus.ACTIVE).build();
        when(productRepository.findByStatus(ListingStatus.ACTIVE)).thenReturn(List.of(active));

        assertThat(productService.getAll()).hasSize(1);
    }

    // ── markAsSold ────────────────────────────────────────────────────────────

    @Test
    void markAsSold_setsStatusToSold_forOwner() {
        Product product = Product.builder()
                .id("prod-1")
                .sellerEmail("owner@example.com")
                .status(ListingStatus.ACTIVE)
                .build();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<ProductDTO> result = productService.markAsSold("prod-1", "owner@example.com", false);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ListingStatus.SOLD);
    }

    // ── decrementStock ────────────────────────────────────────────────────────

    @Test
    void decrementStock_returnsUpdatedProduct_onSuccess() {
        Product updated = Product.builder().id("prod-1").stock(4).build();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(), eq(Product.class)))
                .thenReturn(updated);

        ProductDTO result = productService.decrementStock("prod-1", 1);
        assertThat(result.getStock()).isEqualTo(4);
    }

    @Test
    void decrementStock_throwsIllegalState_whenInsufficientStock() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(), eq(Product.class)))
                .thenReturn(null);
        when(productRepository.existsById("prod-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.decrementStock("prod-1", 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock insuffisant");
    }

    @Test
    void decrementStock_throwsIllegalArgument_whenProductNotFound() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(), eq(Product.class)))
                .thenReturn(null);
        when(productRepository.existsById("ghost")).thenReturn(false);

        assertThatThrownBy(() -> productService.decrementStock("ghost", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Produit introuvable");
    }
}
