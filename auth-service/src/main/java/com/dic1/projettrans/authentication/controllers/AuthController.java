package com.dic1.projettrans.authentication.controllers;

import com.dic1.projettrans.authentication.dto.*;
import com.dic1.projettrans.authentication.filters.TokenBlacklist;
import com.dic1.projettrans.authentication.services.AuthService;
import com.dic1.projettrans.authentication.services.AuthServiceImpl;
import com.dic1.projettrans.authentication.services.TokenService;
import com.dic1.projettrans.authentication.services.TokenServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> register(@RequestParam String telephone) {
        authService.initRegister(telephone);
        return ResponseEntity.ok("OTP envoyé");
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPhone(@RequestBody PhoneCheckDTO request) {
        boolean valid = authService.checkPhone(request);
        return valid ? ResponseEntity.ok("Vérification Réussie") : ResponseEntity.status(403).body("OTP invalide");
    }

    @PostMapping("/register2")
    public ResponseEntity<?> register2(@RequestBody RegisterDTO request) {
        boolean valid = authService.completeRegister(request);
        return valid ? ResponseEntity.ok("Inscription Réussie") : ResponseEntity.status(403).body("Infos invalides");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        boolean login = authService.loginUser(loginDTO);
        if (login) {
            String token = tokenService.generateToken(loginDTO);
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(403).body("Utilisateur inconnu");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/register")
    public ResponseEntity<?> adminCreateUser(@RequestBody RegisterDTO request) {
        boolean created = authService.completeRegister(request);
        return created
                ? ResponseEntity.ok("Utilisateur créé avec succès")
                : ResponseEntity.status(500).body("Erreur lors de la création");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Token manquant");
        }
        tokenBlacklist.add(token);
        return ResponseEntity.ok("Déconnexion réussie");
    }
}
