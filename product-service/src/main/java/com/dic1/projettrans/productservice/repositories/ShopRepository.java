package com.dic1.projettrans.productservice.repositories;

import com.dic1.projettrans.productservice.entities.Shop;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ShopRepository extends MongoRepository<Shop, String> {
    Optional<Shop> findBySlug(String slug);
    Optional<Shop> findByOwnerId(String ownerId);
    boolean existsBySlug(String slug);
    boolean existsByOwnerId(String ownerId);
}
