package com.dic1.projettrans.customerservice.services.impl;

import com.dic1.projettrans.customerservice.entities.Otp;
import com.dic1.projettrans.customerservice.repositories.OtpRepository;
import com.dic1.projettrans.customerservice.services.OtpService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final SecureRandom random = new SecureRandom();
    private final OtpRepository otpRepository;

    public OtpServiceImpl(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Override
    @Transactional
    public String generate(String telephone) {
        Objects.requireNonNull(telephone, "telephone ne doit pas être null");
        String normalized = normalize(telephone);
        if (otpRepository.findTopByTelephoneAndUsedFalseOrderByCreatedAtDesc(normalized).isPresent()) {
            otpRepository.deleteByTelephone(normalized);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));

        LocalDateTime now = LocalDateTime.now();
        Otp otp = new Otp();
        otp.setTelephone(normalized);
        otp.setCode(code);
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plusMinutes(5));
        otp.setUsed(false);
        otpRepository.save(otp);
        return code;
    }

    @Override
    @Transactional
    public boolean verify(String telephone, String code) {
        if (telephone == null || code == null) return false;
        String normalized = normalize(telephone);
        return otpRepository.findTopByTelephoneAndUsedFalseOrderByCreatedAtDesc(normalized)
                .map(otp -> {
                    if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
                        otpRepository.deleteByTelephone(normalized);
                        return false;
                    }
                    boolean match = otp.getCode().equals(code);
                    if (match) {
                        otp.setUsed(true);
                        otpRepository.save(otp);
                    }
                    return match;
                })
                .orElse(false);
    }

    private String normalize(String telephone) {
        return telephone.trim();
    }
}
