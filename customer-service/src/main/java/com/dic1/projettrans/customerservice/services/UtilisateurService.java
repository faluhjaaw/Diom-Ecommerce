package com.dic1.projettrans.customerservice.services;

public interface UtilisateurService {

    boolean verifyCredentials(String telephone, String password);

    String generateOtpForTelephone(String telephone);

    boolean verifyOtp(String telephone, String code);
}
