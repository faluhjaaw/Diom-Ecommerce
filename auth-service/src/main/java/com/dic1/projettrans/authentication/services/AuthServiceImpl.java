package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.LoginDTO;
import com.dic1.projettrans.authentication.dto.MailCheckDTO;
import com.dic1.projettrans.authentication.dto.RegisterDTO;
import com.dic1.projettrans.authentication.entities.User;
import com.dic1.projettrans.authentication.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpServiceImpl otpService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, OtpServiceImpl otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Override
    public void initRegister(RegisterDTO registerDTO) {
        userRepository.findByEmail(registerDTO.getEmail())
                .ifPresent(u -> {
                    throw new RuntimeException("Un utilisateur avec cet email existe déjà");
                });

        otpService.sendOtp(registerDTO.getEmail());
    }

    /**
     * Étape 2 : Validation OTP + Enregistrement en base
     */
    @Override
    public boolean completeRegister(RegisterDTO registerDTO, String otpCode) {
        boolean verified = otpService.verifyOtp(registerDTO.getEmail(), otpCode);

        if (!verified) {
            throw new RuntimeException("OTP invalide ou expiré");
        }

        User user = new User();
        user.setPrenom(registerDTO.getPrenom());
        user.setNom(registerDTO.getNom());
        user.setEmail(registerDTO.getEmail());
        user.setRole(registerDTO.getRole());
        user.setMotDePasse(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmailConfirme(true);
        userRepository.save(user);

        return true;
    }

    /**
     * Connexion
     */
    @Override
    public boolean loginUser(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("Email incorrect"));

        return user.getMotDePasse() != null && passwordEncoder.matches(loginDTO.getPassword(), user.getMotDePasse());
    }
}
