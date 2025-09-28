package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.MailCheckDTO;
import com.dic1.projettrans.authentication.entities.OtpCode;
import com.dic1.projettrans.authentication.repositories.OtpRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpServiceImpl implements OtpService {
    public OtpRepository otpRepository;
    public OtpServiceImpl(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Override
    public void sendOtp(String email) {
        String otp = String.valueOf(new Random().nextInt(899999) + 100000);

        /*Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(fromNumber),
                "Votre code de vérification est : " + otp
        ).create();*/
        System.out.println("============================================================");
        System.out.println("Code OTP pour " + email + ": " + otp);

        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(email);
        otpCode.setCode(otp);
        otpCode.setExpiration(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otpCode);
    }

    @Override
    public boolean verifyOtp(String email, String codeOTP) {
        return otpRepository.findTopByEmailOrderByExpirationDesc(email)
                .filter(otpCode -> otpCode.getCode().equals(codeOTP) && otpCode.getExpiration().isAfter(LocalDateTime.now()))
                .isPresent();
    }
}
