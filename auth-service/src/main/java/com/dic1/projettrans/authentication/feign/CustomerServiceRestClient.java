package com.dic1.projettrans.authentication.feign;

import com.dic1.projettrans.authentication.dto.MailCheckDTO;
import com.dic1.projettrans.authentication.dto.OtpGenerateDTO;
import com.dic1.projettrans.authentication.dto.Utilisateur;
import com.dic1.projettrans.authentication.model.CustomerUser;
import com.dic1.projettrans.authentication.model.CredentialRequest;
import com.dic1.projettrans.authentication.model.Otp;
import com.dic1.projettrans.authentication.model.OtpCheck;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "customer-service")
public interface CustomerServiceRestClient {
    @GetMapping("/api/users")
    List<CustomerUser> findAllUsers();

    @GetMapping("/api/users/email/{email}")
    CustomerUser findUserByEmail(@PathVariable String email);

    @PostMapping("/api/users/verify-credentials")
    ResponseEntity<Map<String, Object>> verifyCredentials(@RequestBody CredentialRequest request);

    @PostMapping("/api/users/otp/generate")
    Otp generateOTP(@RequestParam String email);

    @PostMapping("/api/users/otp/verify")
    OtpCheck verifyOTP(@RequestParam String email, @RequestParam String code);

    @PostMapping("/api/users")
    Utilisateur inscription(@RequestBody Utilisateur user);


}