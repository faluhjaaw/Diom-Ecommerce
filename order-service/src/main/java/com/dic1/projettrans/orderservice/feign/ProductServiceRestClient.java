package com.dic1.projettrans.orderservice.feign;

import com.dic1.projettrans.orderservice.model.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductServiceRestClient {
    @GetMapping("/api/products/{id}")
    Product findProductById(@PathVariable("id") String id);
}
