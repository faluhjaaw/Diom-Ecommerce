package com.dic1.projettrans.authentication.controllers;

import com.dic1.projettrans.authentication.dto.LoginDTO;
import com.dic1.projettrans.authentication.dto.MailCheckDTO;
import com.dic1.projettrans.authentication.dto.RegisterDTO;
import com.dic1.projettrans.authentication.dto.RegisterWithOtpDTO;
import com.dic1.projettrans.authentication.filters.TokenBlacklist;
import com.dic1.projettrans.authentication.services.AuthService;
import com.dic1.projettrans.authentication.services.AuthServiceImpl;
import com.dic1.projettrans.authentication.services.TokenService;
import com.dic1.projettrans.authentication.services.TokenServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final TokenBlacklist tokenBlacklist;

    public AuthController(AuthService authService, TokenService tokenService, TokenBlacklist tokenBlacklist) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.tokenBlacklist = tokenBlacklist;
    }

    @PostMapping("/register1")
    public ResponseEntity<?> register(@RequestBody RegisterDTO registerDTO) {
        authService.initRegister(registerDTO);
        return ResponseEntity.ok("OTP envoyé");
    }

    @PostMapping("/register2")
    public ResponseEntity<?> verifyMail(@RequestBody RegisterWithOtpDTO request) {
        boolean valid = authService.completeRegister(request.getRegisterDTO(), request.getOtpCode());
        return valid ? ResponseEntity.ok("Inscription Réussie") : ResponseEntity.status(403).body("OTP invalide");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        boolean login = authService.loginUser(loginDTO);
        if(login){
            String token = tokenService.generateToken(loginDTO);
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(403).body("Utilisateur inconnue");
    }
}
