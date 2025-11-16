package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.*;
import com.dic1.projettrans.authentication.entities.User;
import com.dic1.projettrans.authentication.feign.CustomerServiceRestClient;
import com.dic1.projettrans.authentication.model.CustomerUser;
import com.dic1.projettrans.authentication.model.Otp;
import com.dic1.projettrans.authentication.model.OtpCheck;
import com.dic1.projettrans.authentication.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final CustomerServiceRestClient customerClient;

    public AuthServiceImpl(PasswordEncoder passwordEncoder, CustomerServiceRestClient customerClient) {
        this.passwordEncoder = passwordEncoder;
        this.customerClient = customerClient;
    }

    @Override
    public void initRegister(String email) {
        CustomerUser user = null;
        try {
            user = customerClient.findUserByEmail(email);
        } catch (Exception e) {
            System.out.println("Utilisateur introuvable !");
        }

        if(user != null) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        Otp otp = customerClient.generateOTP(email);

        System.out.println("============================================================");
        System.out.println("Code OTP pour " + email + ": " + otp.getOtp());
    }

    @Override
    public boolean checkMail(MailCheckDTO mailCheckDTO) {
        OtpCheck verified = customerClient.verifyOTP(mailCheckDTO.getEmail(), mailCheckDTO.getCode());
        if (!verified.isValid()) {
            throw new RuntimeException("OTP invalide ou expiré");
        }
        return true;
    }

    /**
     * Étape 2 : Validation OTP + Enregistrement en base
     */
    @Override
    public boolean completeRegister(RegisterDTO registerDTO) {

        Utilisateur user = new Utilisateur();
        user.setPrenom(registerDTO.getPrenom());
        user.setNom(registerDTO.getNom());
        user.setTelephone(registerDTO.getTelephone());
        user.setAdresse(registerDTO.getAdresse());
        user.setEmail(registerDTO.getEmail());
        user.setRole(registerDTO.getRole());
        user.setMotDePasse(passwordEncoder.encode(registerDTO.getPassword()));
        customerClient.inscription(user);

        return true;
    }

    /**
     * Connexion
     */
    @Override
    public boolean loginUser(LoginDTO loginDTO) {
        // Use customer-service data to authenticate
        CustomerUser user;
        try {
            user = customerClient.findUserByEmail(loginDTO.getEmail());
        } catch (Exception e) {
            throw new RuntimeException("Service client indisponible");
        }

        String hashed = user.getMotDePasse();
        return hashed != null && passwordEncoder.matches(loginDTO.getPassword(), hashed);
    }
}
