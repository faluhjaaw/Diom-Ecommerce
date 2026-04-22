package com.dic1.projettrans.orderservice.feign;

import com.dic1.projettrans.orderservice.model.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductServiceRestClient {
    @GetMapping("/api/products/{id}")
    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackFindProductById")
    Product findProductById(@PathVariable("id") String id);

    @PatchMapping("/api/products/{id}/decrement-stock")
    void decrementStock(@PathVariable("id") String id, @RequestParam("quantity") int quantity);

    default Product fallbackFindProductById(String id, Exception e) {
        return Product.builder().id(id).build();
    }
}
