package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.*;
import com.dic1.projettrans.authentication.feign.CustomerServiceRestClient;
import com.dic1.projettrans.authentication.model.CredentialRequest;
import com.dic1.projettrans.authentication.model.OtpCheck;
import feign.FeignException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final CustomerServiceRestClient customerClient;

    public AuthServiceImpl(PasswordEncoder passwordEncoder, CustomerServiceRestClient customerClient) {
        this.passwordEncoder = passwordEncoder;
        this.customerClient = customerClient;
    }

    @Override
    public void initRegister(String telephone) {
        customerClient.generateOTP(telephone);
    }

    @Override
    public boolean checkPhone(PhoneCheckDTO phoneCheckDTO) {
        OtpCheck verified = customerClient.verifyOTP(phoneCheckDTO.getTelephone(), phoneCheckDTO.getOtp());
        return verified != null && verified.isValid();
    }

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

    @Override
    public boolean loginUser(LoginDTO loginDTO) {
        try {
            var resp = customerClient.verifyCredentials(
                    new CredentialRequest(loginDTO.getTelephone(), loginDTO.getPassword()));
            return resp != null && Boolean.TRUE.equals(
                    resp.getBody() != null ? resp.getBody().get("valid") : false);
        } catch (FeignException.Unauthorized | FeignException.BadRequest e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Service client indisponible");
        }
    }
}
