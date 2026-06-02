package com.dic1.projettrans.customerservice.controllers;

import com.dic1.projettrans.customerservice.controllers.dto.CredentialRequest;
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
        boolean ok = utilisateurService.verifyCredentials(req.getTelephone(), req.getPassword());
        Map<String, Object> resp = new HashMap<>();
        resp.put("telephone", req.getTelephone());
        resp.put("valid", ok);
        return ok ? ResponseEntity.ok(resp) : ResponseEntity.status(401).body(resp);
    }

    @PostMapping("/otp/generate")
    public ResponseEntity<Map<String, Object>> generateOtp(@RequestParam String telephone) {
        utilisateurService.generateOtpForTelephone(telephone);
        Map<String, Object> resp = new HashMap<>();
        resp.put("telephone", telephone);
        resp.put("message", "OTP généré (valide 5 minutes)");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestParam String telephone, @RequestParam String code) {
        boolean ok = utilisateurService.verifyOtp(telephone, code);
        Map<String, Object> resp = new HashMap<>();
        resp.put("telephone", telephone);
        resp.put("valid", ok);
        return ok ? ResponseEntity.ok(resp) : ResponseEntity.status(400).body(resp);
    }
}
