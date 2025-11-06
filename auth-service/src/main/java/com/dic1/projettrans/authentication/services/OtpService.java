package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.MailCheckDTO;

public interface OtpService {
    void sendOtp(String email);

    boolean verifyOtp(String email, String otpCode);
}
