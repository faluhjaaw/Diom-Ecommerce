package com.dic1.projettrans.orderservice.feign;

import com.dic1.projettrans.orderservice.config.FeignInternalAuthConfig;
import com.dic1.projettrans.orderservice.model.Cart;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service", configuration = FeignInternalAuthConfig.class)
public interface CartServiceRestClient {
    @GetMapping("/api/carts/internal/{userId}")
    @CircuitBreaker(name = "cart-service", fallbackMethod = "fallbackGetCart")
    Cart getCart(@PathVariable("userId") Long userId);

    default Cart fallbackGetCart(Long userId, Exception e) {
        e.printStackTrace();
        return null;
    }

    @DeleteMapping("/api/carts/internal/{userId}")
    @CircuitBreaker(name = "cart-service", fallbackMethod = "fallbackClearCart")
    Cart clearCart(@PathVariable("userId") Long userId);

    default Cart fallbackClearCart(Long userId, Exception e) {
        e.printStackTrace();
        return null;
    }
}
