package com.dic1.projettrans.customerservice.controllers;

import com.dic1.projettrans.customerservice.controllers.dto.CredentialRequest;
import com.dic1.projettrans.customerservice.controllers.dto.OtpGenerateRequest;
import com.dic1.projettrans.customerservice.controllers.dto.OtpVerifyRequest;
import com.dic1.projettrans.customerservice.services.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UtilisateurAuthController {

    private final UtilisateurService utilisateurService;

    public UtilisateurAuthController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @PostMapping("/verify-credentials")
    public ResponseEntity<Map<String, Object>> verifyCredentials(@RequestBody CredentialRequest req) {
        boolean ok = utilisateurService.verifyCredentials(req.getEmail(), req.getPassword());
        Map<String, Object> resp = new HashMap<>();
        resp.put("email", req.getEmail());
        resp.put("valid", ok);
        return ok ? ResponseEntity.ok(resp) : ResponseEntity.status(401).body(resp);
    }

    @PostMapping("/otp/generate")
    public ResponseEntity<Map<String, Object>> generateOtp(@RequestBody OtpGenerateRequest req) {
        String code = utilisateurService.generateOtpForEmail(req.getEmail());
        Map<String, Object> resp = new HashMap<>();
        resp.put("email", req.getEmail());
        resp.put("otp", code);
        resp.put("message", "OTP généré (valide 5 minutes)");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestParam String email, @RequestParam String code) {
        boolean ok = utilisateurService.verifyOtp(email, code);
        Map<String, Object> resp = new HashMap<>();
        resp.put("email", email);
        resp.put("valid", ok);
        return ok ? ResponseEntity.ok(resp) : ResponseEntity.status(400).body(resp);
    }
}
