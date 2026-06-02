package com.dic1.projettrans.customerservice;

import com.dic1.projettrans.customerservice.entities.Utilisateur;
import com.dic1.projettrans.customerservice.repositories.UtilisateurRepository;
import com.dic1.projettrans.customerservice.services.OtpService;
import com.dic1.projettrans.customerservice.services.impl.UtilisateurServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private OtpService otpService;
    @InjectMocks
    private UtilisateurServiceImpl service;

    // ── verifyCredentials ─────────────────────────────────────────────────────

    @Test
    void verifyCredentials_returnsTrue_whenPasswordMatches() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashed = encoder.encode("correct");

        Utilisateur user = new Utilisateur();
        user.setTelephone("+221771234567");
        user.setMotDePasse(hashed);
        user.setActive(true);

        when(utilisateurRepository.findByTelephoneAndActiveTrue("+221771234567"))
                .thenReturn(Optional.of(user));

        assertThat(service.verifyCredentials("+221771234567", "correct")).isTrue();
    }

    @Test
    void verifyCredentials_returnsFalse_whenPasswordWrong() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashed = encoder.encode("correct");

        Utilisateur user = new Utilisateur();
        user.setMotDePasse(hashed);

        when(utilisateurRepository.findByTelephoneAndActiveTrue("+221771234567"))
                .thenReturn(Optional.of(user));

        assertThat(service.verifyCredentials("+221771234567", "wrong")).isFalse();
    }

    @Test
    void verifyCredentials_returnsFalse_whenUserNotFound() {
        when(utilisateurRepository.findByTelephoneAndActiveTrue(any())).thenReturn(Optional.empty());

        assertThat(service.verifyCredentials("+221771234567", "any")).isFalse();
    }

    @Test
    void verifyCredentials_returnsFalse_whenMotDePasseNull() {
        Utilisateur user = new Utilisateur();
        user.setMotDePasse(null);

        when(utilisateurRepository.findByTelephoneAndActiveTrue(any())).thenReturn(Optional.of(user));

        assertThat(service.verifyCredentials("+221771234567", "pass")).isFalse();
    }

    // ── generateOtpForTelephone ───────────────────────────────────────────────

    @Test
    void generateOtp_delegatesToOtpService_whenPhoneNotRegistered() {
        when(utilisateurRepository.findByTelephone("+221771234567")).thenReturn(Optional.empty());
        when(otpService.generate("+221771234567")).thenReturn("654321");

        String code = service.generateOtpForTelephone("+221771234567");
        assertThat(code).isEqualTo("654321");
    }

    @Test
    void generateOtp_throwsConflict_whenPhoneAlreadyRegistered() {
        Utilisateur existing = new Utilisateur();
        existing.setTelephone("+221771234567");

        when(utilisateurRepository.findByTelephone("+221771234567"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.generateOtpForTelephone("+221771234567"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(otpService, never()).generate(any());
    }

    // ── verifyOtp ─────────────────────────────────────────────────────────────

    @Test
    void verifyOtp_delegatesToOtpService() {
        when(otpService.verify("+221771234567", "123456")).thenReturn(true);

        assertThat(service.verifyOtp("+221771234567", "123456")).isTrue();
        verify(otpService).verify("+221771234567", "123456");
    }

    @Test
    void verifyOtp_returnsFalse_whenOtpInvalid() {
        when(otpService.verify(any(), any())).thenReturn(false);

        assertThat(service.verifyOtp("+221771234567", "000000")).isFalse();
    }
}
