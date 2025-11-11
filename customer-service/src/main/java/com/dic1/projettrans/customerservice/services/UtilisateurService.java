package com.dic1.projettrans.customerservice.services;

public interface UtilisateurService {

    boolean verifyCredentials(String email, String password);

    String generateOtpForEmail(String email);

    boolean verifyOtp(String email, String code);
}
