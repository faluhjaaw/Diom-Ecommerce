package com.dic1.projettrans.authentication.feign;

import com.dic1.projettrans.authentication.model.CustomerUser;
import com.dic1.projettrans.authentication.model.CredentialRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "customer-service")
public interface CustomerServiceRestClient {
    @GetMapping("/api/users")
    List<CustomerUser> findAllUsers();

    @PostMapping("/api/users/verify-credentials")
    ResponseEntity<Map<String, Object>> verifyCredentials(@RequestBody CredentialRequest request);
}