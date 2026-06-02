package com.dic1.projettrans.productservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class CustomerServiceClient {

    private final RestTemplate restTemplate;

    @Value("${customer.service.url:http://customer-service:8083}")
    private String customerServiceUrl;

    public CustomerServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<UserResponse> getUserByEmail(String email) {
        try {
            UserResponse user = restTemplate.getForObject(
                    customerServiceUrl + "/api/users/email/" + email,
                    UserResponse.class
            );
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
