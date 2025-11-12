package com.dic1.projettrans.cartservice.feign;

import com.dic1.projettrans.cartservice.model.Customer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerServiceRestClient {
    @GetMapping("/api/users/{id}")
    Customer findUserById(@PathVariable("id") Long id);
}
