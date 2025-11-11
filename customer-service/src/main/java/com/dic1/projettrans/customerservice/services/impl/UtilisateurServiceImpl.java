package com.dic1.projettrans.customerservice.services.impl;

import com.dic1.projettrans.customerservice.entities.Utilisateur;
import com.dic1.projettrans.customerservice.repositories.UtilisateurRepository;
import com.dic1.projettrans.customerservice.services.OtpService;
import com.dic1.projettrans.customerservice.services.UtilisateurService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final OtpService otpService;

    public UtilisateurServiceImpl(UtilisateurRepository utilisateurRepository, OtpService otpService) {
        this.utilisateurRepository = utilisateurRepository;
        this.otpService = otpService;
    }

    @Override
    public boolean verifyCredentials(String email, String password) {
        Optional<Utilisateur> opt = utilisateurRepository.findByEmail(email);
        return opt.filter(u -> u.getMotDePasse() != null && u.getMotDePasse().equals(password)).isPresent();
    }

    @Override
    public String generateOtpForEmail(String email) {
        // Ensure the user exists before generating an OTP
        utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable pour l'email: " + email));
        return otpService.generate(email);
    }

    @Override
    public boolean verifyOtp(String email, String code) {
        // Ensure the user exists before verifying an OTP
        Optional<Utilisateur> opt = utilisateurRepository.findByEmail(email);
        if (opt.isEmpty()) return false;
        return otpService.verify(email, code);
    }
}
