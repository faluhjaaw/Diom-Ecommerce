package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.*;
import com.dic1.projettrans.authentication.entities.User;
import com.dic1.projettrans.authentication.feign.CustomerServiceRestClient;
import com.dic1.projettrans.authentication.model.CredentialRequest;
import com.dic1.projettrans.authentication.model.CustomerUser;
import com.dic1.projettrans.authentication.model.Otp;
import com.dic1.projettrans.authentication.model.OtpCheck;
import com.dic1.projettrans.authentication.repositories.UserRepository;
import feign.FeignException;
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
            // user not found — continue to OTP generation
        }

        if(user != null) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        customerClient.generateOTP(email);
    }

    @Override
    public boolean checkMail(MailCheckDTO mailCheckDTO) {
        OtpCheck verified = customerClient.verifyOTP(mailCheckDTO.getEmail(), mailCheckDTO.getOtp());
        return verified != null && verified.isValid();
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
        try {
            var resp = customerClient.verifyCredentials(
                    new CredentialRequest(loginDTO.getEmail(), loginDTO.getPassword()));
            return resp != null && Boolean.TRUE.equals(
                    resp.getBody() != null ? resp.getBody().get("valid") : false);
        } catch (FeignException.Unauthorized | FeignException.BadRequest e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Service client indisponible");
        }
    }
}
