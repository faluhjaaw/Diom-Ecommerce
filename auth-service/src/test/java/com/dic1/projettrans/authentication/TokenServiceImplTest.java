package com.dic1.projettrans.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.dic1.projettrans.authentication.dto.LoginDTO;
import com.dic1.projettrans.authentication.enums.Role;
import com.dic1.projettrans.authentication.feign.CustomerServiceRestClient;
import com.dic1.projettrans.authentication.model.CustomerUser;
import com.dic1.projettrans.authentication.services.TokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private CustomerServiceRestClient customerClient;
    @InjectMocks
    private TokenServiceImpl tokenService;

    private static final String TEST_SECRET = "TestSecretKeyForUnitTests123456";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "jwtSecret", TEST_SECRET);
    }

    @Test
    void generateToken_containsEmailAsSubject() {
        CustomerUser user = new CustomerUser(1L, "Moussa", "Diallo",
                "moussa@example.com", "+221771234567", null, null, Role.CUSTOMER);
        when(customerClient.findUserByTelephone("+221771234567")).thenReturn(user);

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221771234567");

        String token = tokenService.generateToken(login);

        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(TEST_SECRET)).build().verify(token);
        assertThat(decoded.getSubject()).isEqualTo("moussa@example.com");
    }

    @Test
    void generateToken_containsRoleClaim() {
        CustomerUser user = new CustomerUser(1L, "Admin", "User",
                "admin@example.com", "+221771111111", null, null, Role.ADMIN);
        when(customerClient.findUserByTelephone("+221771111111")).thenReturn(user);

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221771111111");

        String token = tokenService.generateToken(login);

        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(TEST_SECRET)).build().verify(token);
        assertThat(decoded.getClaim("role").asString()).isEqualTo("ADMIN");
    }

    @Test
    void generateToken_containsUserId() {
        CustomerUser user = new CustomerUser(42L, "Test", "User",
                "test@example.com", "+221770000000", null, null, Role.CUSTOMER);
        when(customerClient.findUserByTelephone("+221770000000")).thenReturn(user);

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221770000000");

        String token = tokenService.generateToken(login);

        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(TEST_SECRET)).build().verify(token);
        assertThat(decoded.getClaim("userId").asLong()).isEqualTo(42L);
    }

    @Test
    void generateToken_defaultsToCustomer_whenRoleNull() {
        CustomerUser user = new CustomerUser(5L, "No", "Role",
                "norole@example.com", "+221770001111", null, null, null);
        when(customerClient.findUserByTelephone("+221770001111")).thenReturn(user);

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221770001111");

        String token = tokenService.generateToken(login);

        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(TEST_SECRET)).build().verify(token);
        assertThat(decoded.getClaim("role").asString()).isEqualTo("CUSTOMER");
    }

    @Test
    void generateToken_expiresIn24Hours() {
        CustomerUser user = new CustomerUser(1L, "X", "Y",
                "x@example.com", "+221770002222", null, null, Role.CUSTOMER);
        when(customerClient.findUserByTelephone("+221770002222")).thenReturn(user);

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221770002222");

        long before = System.currentTimeMillis();
        String token = tokenService.generateToken(login);

        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(TEST_SECRET)).build().verify(token);
        Date expiry = decoded.getExpiresAt();
        long expected24h = before + 1000L * 60 * 60 * 24;

        // JWT truncates to seconds — allow ±2s tolerance
        assertThat(expiry.getTime()).isBetween(expected24h - 2000, expected24h + 2000);
    }
}
