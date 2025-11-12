package com.dic1.projettrans.cartservice.repositories;

import com.dic1.projettrans.cartservice.entities.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
