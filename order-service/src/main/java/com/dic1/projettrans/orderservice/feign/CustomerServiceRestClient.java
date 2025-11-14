package com.dic1.projettrans.orderservice.feign;

import com.dic1.projettrans.orderservice.model.Customer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerServiceRestClient {
    @GetMapping("/api/users/id/{id}")
    @CircuitBreaker(name = "customer-service", fallbackMethod = "fallbackFindUserById")
    Customer findUserById(@PathVariable("id") Long id);

    default Customer fallbackFindUserById(Long id, Exception e) {
        e.printStackTrace();
        return Customer.builder().id(id).build();
    }
}
