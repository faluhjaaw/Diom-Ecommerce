package com.dic1.projettrans.authentication;

import com.dic1.projettrans.authentication.dto.*;
import com.dic1.projettrans.authentication.feign.CustomerServiceRestClient;
import com.dic1.projettrans.authentication.model.CredentialRequest;
import com.dic1.projettrans.authentication.model.OtpCheck;
import com.dic1.projettrans.authentication.services.AuthServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CustomerServiceRestClient customerClient;
    @InjectMocks
    private AuthServiceImpl authService;

    // ── initRegister ──────────────────────────────────────────────────────────

    @Test
    void initRegister_delegatesOtpGeneration() {
        authService.initRegister("+221771234567");
        verify(customerClient).generateOTP("+221771234567");
    }

    // ── checkPhone ────────────────────────────────────────────────────────────

    @Test
    void checkPhone_returnsTrue_whenOtpValid() {
        OtpCheck valid = new OtpCheck(true, "+221771234567");
        when(customerClient.verifyOTP("+221771234567", "123456")).thenReturn(valid);

        PhoneCheckDTO dto = new PhoneCheckDTO("+221771234567", "123456");
        assertThat(authService.checkPhone(dto)).isTrue();
    }

    @Test
    void checkPhone_returnsFalse_whenOtpInvalid() {
        OtpCheck invalid = new OtpCheck(false, "+221771234567");
        when(customerClient.verifyOTP("+221771234567", "000000")).thenReturn(invalid);

        PhoneCheckDTO dto = new PhoneCheckDTO("+221771234567", "000000");
        assertThat(authService.checkPhone(dto)).isFalse();
    }

    @Test
    void checkPhone_returnsFalse_whenClientReturnsNull() {
        when(customerClient.verifyOTP(any(), any())).thenReturn(null);

        PhoneCheckDTO dto = new PhoneCheckDTO("+221771234567", "123456");
        assertThat(authService.checkPhone(dto)).isFalse();
    }

    // ── completeRegister ──────────────────────────────────────────────────────

    @Test
    void completeRegister_encodesPasswordAndCallsInscription() {
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        RegisterDTO dto = new RegisterDTO();
        dto.setPrenom("Moussa");
        dto.setNom("Diallo");
        dto.setEmail("moussa@example.com");
        dto.setTelephone("+221771234567");
        dto.setPassword("secret");

        boolean result = authService.completeRegister(dto);

        assertThat(result).isTrue();
        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(customerClient).inscription(captor.capture());
        Utilisateur sent = captor.getValue();
        assertThat(sent.getMotDePasse()).isEqualTo("hashed");
        assertThat(sent.getEmail()).isEqualTo("moussa@example.com");
        assertThat(sent.getTelephone()).isEqualTo("+221771234567");
    }

    // ── loginUser ─────────────────────────────────────────────────────────────

    @Test
    void loginUser_returnsTrue_whenCredentialsValid() {
        ResponseEntity<Map<String, Object>> resp =
                ResponseEntity.ok(Map.of("valid", true, "telephone", "+221771234567"));
        when(customerClient.verifyCredentials(any(CredentialRequest.class))).thenReturn(resp);

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221771234567");
        login.setPassword("secret");

        assertThat(authService.loginUser(login)).isTrue();
    }

    @Test
    void loginUser_returnsFalse_whenCredentialsInvalid() {
        ResponseEntity<Map<String, Object>> resp =
                ResponseEntity.ok(Map.of("valid", false));
        when(customerClient.verifyCredentials(any(CredentialRequest.class))).thenReturn(resp);

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221771234567");
        login.setPassword("wrong");

        assertThat(authService.loginUser(login)).isFalse();
    }

    @Test
    void loginUser_returnsFalse_onFeignUnauthorized() {
        when(customerClient.verifyCredentials(any()))
                .thenThrow(mock(FeignException.Unauthorized.class));

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221771234567");
        login.setPassword("bad");

        assertThat(authService.loginUser(login)).isFalse();
    }

    @Test
    void loginUser_throwsRuntime_onServiceUnavailable() {
        when(customerClient.verifyCredentials(any()))
                .thenThrow(new RuntimeException("timeout"));

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221771234567");
        login.setPassword("bad");

        assertThatThrownBy(() -> authService.loginUser(login))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Service client indisponible");
    }

    @Test
    void loginUser_usesPhoneFromDto() {
        ResponseEntity<Map<String, Object>> resp =
                ResponseEntity.ok(Map.of("valid", true));
        when(customerClient.verifyCredentials(any(CredentialRequest.class))).thenReturn(resp);

        LoginDTO login = new LoginDTO();
        login.setTelephone("+221779999999");
        login.setPassword("pass");

        authService.loginUser(login);

        ArgumentCaptor<CredentialRequest> captor = ArgumentCaptor.forClass(CredentialRequest.class);
        verify(customerClient).verifyCredentials(captor.capture());
        assertThat(captor.getValue().getTelephone()).isEqualTo("+221779999999");
    }
}
