package com.dic1.projettrans.customerservice;

import com.dic1.projettrans.customerservice.entities.Otp;
import com.dic1.projettrans.customerservice.repositories.OtpRepository;
import com.dic1.projettrans.customerservice.services.impl.OtpServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private OtpRepository otpRepository;
    @InjectMocks
    private OtpServiceImpl otpService;

    // ── generate ──────────────────────────────────────────────────────────────

    @Test
    void generate_savesNewOtp_whenNoneExists() {
        when(otpRepository.findTopByTelephoneAndUsedFalseOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());
        when(otpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String code = otpService.generate("+221771234567");

        assertThat(code).hasSize(6).matches("\\d{6}");
        ArgumentCaptor<Otp> captor = ArgumentCaptor.forClass(Otp.class);
        verify(otpRepository).save(captor.capture());
        Otp saved = captor.getValue();
        assertThat(saved.getTelephone()).isEqualTo("+221771234567");
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(saved.getCreatedAt());
    }

    @Test
    void generate_deletesOldOtp_whenOneAlreadyExists() {
        Otp existing = new Otp();
        existing.setTelephone("+221771234567");
        when(otpRepository.findTopByTelephoneAndUsedFalseOrderByCreatedAtDesc("+221771234567"))
                .thenReturn(Optional.of(existing));
        when(otpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        otpService.generate("+221771234567");

        verify(otpRepository).deleteByTelephone("+221771234567");
    }

    @Test
    void generate_trimsWhitespace() {
        when(otpRepository.findTopByTelephoneAndUsedFalseOrderByCreatedAtDesc("+221771234567"))
                .thenReturn(Optional.empty());
        when(otpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        otpService.generate("  +221771234567  ");

        ArgumentCaptor<Otp> captor = ArgumentCaptor.forClass(Otp.class);
        verify(otpRepository).save(captor.capture());
        assertThat(captor.getValue().getTelephone()).isEqualTo("+221771234567");
    }

    @Test
    void generate_throwsNullPointer_whenTelephoneNull() {
        assertThatThrownBy(() -> otpService.generate(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ── verify ────────────────────────────────────────────────────────────────

    @Test
    void verify_returnsTrue_whenCodeMatchesAndNotExpired() {
        Otp otp = new Otp();
        otp.setTelephone("+221771234567");
        otp.setCode("123456");
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(4));

        when(otpRepository.findTopByTelephoneAndUsedFalseOrderByCreatedAtDesc("+221771234567"))
                .thenReturn(Optional.of(otp));
        when(otpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(otpService.verify("+221771234567", "123456")).isTrue();
        assertThat(otp.isUsed()).isTrue();
        verify(otpRepository).save(otp);
    }

    @Test
    void verify_returnsFalse_whenCodeWrong() {
        Otp otp = new Otp();
        otp.setCode("123456");
        otp.setUsed(false);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(4));

        when(otpRepository.findTopByTelephoneAndUsedFalseOrderByCreatedAtDesc("+221771234567"))
                .thenReturn(Optional.of(otp));

        assertThat(otpService.verify("+221771234567", "999999")).isFalse();
        verify(otpRepository, never()).save(any());
    }

    @Test
    void verify_returnsFalse_whenExpired() {
        Otp otp = new Otp();
        otp.setTelephone("+221771234567");
        otp.setCode("123456");
        otp.setUsed(false);
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired

        when(otpRepository.findTopByTelephoneAndUsedFalseOrderByCreatedAtDesc("+221771234567"))
                .thenReturn(Optional.of(otp));

        assertThat(otpService.verify("+221771234567", "123456")).isFalse();
        verify(otpRepository).deleteByTelephone("+221771234567");
    }

    @Test
    void verify_returnsFalse_whenNoOtpFound() {
        when(otpRepository.findTopByTelephoneAndUsedFalseOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());

        assertThat(otpService.verify("+221771234567", "123456")).isFalse();
    }

    @Test
    void verify_returnsFalse_whenPhoneOrCodeNull() {
        assertThat(otpService.verify(null, "123456")).isFalse();
        assertThat(otpService.verify("+221771234567", null)).isFalse();
    }
}
