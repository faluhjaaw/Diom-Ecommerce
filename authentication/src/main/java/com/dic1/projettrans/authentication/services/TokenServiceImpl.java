package com.dic1.projettrans.authentication.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.dic1.projettrans.authentication.dto.LoginDTO;
import com.dic1.projettrans.authentication.dto.TokenDTO;
import com.dic1.projettrans.authentication.entities.User;
import com.dic1.projettrans.authentication.enums.Role;
import com.dic1.projettrans.authentication.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class TokenServiceImpl implements TokenService {
    private final UserRepository userRepository;

    public TokenServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String generateToken(LoginDTO loginDTO) {
        Optional<User> user = userRepository.findByEmail(loginDTO.getEmail());
        String role = user.get().getRole().toString();
        return JWT.create()
                .withSubject(loginDTO.getEmail())
                .withClaim("role", role)
                .withIssuedAt(new Date())
                //.withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 heure
                .sign(Algorithm.HMAC256("ECommerceSecret123"));
    }
}
