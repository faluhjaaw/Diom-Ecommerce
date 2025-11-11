package com.dic1.projettrans.customerservice.services.impl;

import com.dic1.projettrans.customerservice.entities.Otp;
import com.dic1.projettrans.customerservice.repositories.OtpRepository;
import com.dic1.projettrans.customerservice.services.OtpService;
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
    public String generate(String email) {
        Objects.requireNonNull(email, "email ne doit pas être null");
        String normalized = normalize(email);
        // Optionally remove previous OTPs for this email to keep only the latest
        otpRepository.deleteByEmail(normalized);

        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        Otp otp = new Otp();
        otp.setEmail(normalized);
        otp.setCode(code);
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plus(DEFAULT_TTL));
        otp.setUsed(false);
        otpRepository.save(otp);
        return code;
    }

    @Override
    public boolean verify(String email, String code) {
        if (email == null || code == null) return false;
        String normalized = normalize(email);
        return otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(normalized)
                .map(otp -> {
                    if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
                        // Expired: clean up and refuse
                        otpRepository.deleteByEmail(normalized);
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

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
