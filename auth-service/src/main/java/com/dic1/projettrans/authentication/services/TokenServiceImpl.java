package com.dic1.projettrans.authentication.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.dic1.projettrans.authentication.dto.LoginDTO;
import com.dic1.projettrans.authentication.feign.CustomerServiceRestClient;
import com.dic1.projettrans.authentication.model.CustomerUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenServiceImpl implements TokenService {
    private final CustomerServiceRestClient customerClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public TokenServiceImpl(CustomerServiceRestClient customerClient) {
        this.customerClient = customerClient;
    }

    @Override
    public String generateToken(LoginDTO loginDTO) {
        CustomerUser user = customerClient.findUserByTelephone(loginDTO.getTelephone());
        String role = user.getRole() != null ? user.getRole().name() : "CUSTOMER";
        return JWT.create()
                .withSubject(user.getEmail())
                .withClaim("role", role)
                .withClaim("userId", user.getId())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24)) // 24h
                .sign(Algorithm.HMAC256(jwtSecret));
    }
}
