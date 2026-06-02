package com.dic1.projettrans.customerservice.services.impl;

import com.dic1.projettrans.customerservice.entities.Utilisateur;
import com.dic1.projettrans.customerservice.repositories.UtilisateurRepository;
import com.dic1.projettrans.customerservice.services.OtpService;
import com.dic1.projettrans.customerservice.services.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final OtpService otpService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UtilisateurServiceImpl(UtilisateurRepository utilisateurRepository, OtpService otpService) {
        this.utilisateurRepository = utilisateurRepository;
        this.otpService = otpService;
    }

    @Override
    public boolean verifyCredentials(String telephone, String password) {
        return utilisateurRepository.findByTelephoneAndActiveTrue(telephone)
                .filter(u -> u.getMotDePasse() != null && passwordEncoder.matches(password, u.getMotDePasse()))
                .isPresent();
    }

    @Override
    public String generateOtpForTelephone(String telephone) {
        if (utilisateurRepository.findByTelephone(telephone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un compte existe déjà avec ce numéro de téléphone");
        }
        return otpService.generate(telephone);
    }

    @Override
    public boolean verifyOtp(String telephone, String code) {
        return otpService.verify(telephone, code);
    }
}
