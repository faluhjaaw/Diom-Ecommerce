package com.dic1.projettrans.avisservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/check")
    boolean hasUserOrderedProduct(@RequestParam("userId") Long userId,
                                  @RequestParam("productId") String productId);
}
