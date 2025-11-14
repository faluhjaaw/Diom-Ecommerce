package com.dic1.projettrans.orderservice.feign;

import com.dic1.projettrans.orderservice.model.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductServiceRestClient {
    @GetMapping("/api/products/{id}")
    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackFindProductById")
    Product findProductById(@PathVariable("id") String id);

    default Product fallbackFindProductById(String id, Exception e) {
        e.printStackTrace();
        return Product.builder().id(id).build();
    }
}
