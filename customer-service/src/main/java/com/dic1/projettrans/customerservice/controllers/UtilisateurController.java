package com.dic1.projettrans.customerservice.controllers;

import com.dic1.projettrans.customerservice.entities.Role;
import com.dic1.projettrans.customerservice.entities.SellerType;
import com.dic1.projettrans.customerservice.entities.Utilisateur;
import com.dic1.projettrans.customerservice.repositories.UtilisateurRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurController(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @PostMapping
    public ResponseEntity<Utilisateur> create(@RequestBody Utilisateur user) {
        if (user.getRole() == null) {
            user.setRole(Role.CUSTOMER);
        }
        user.setActive(true);
        Utilisateur saved = utilisateurRepository.save(user);
        return ResponseEntity.created(URI.create("/api/users/" + saved.getId())).body(saved);
    }

    @GetMapping
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findByActiveTrue();
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<Utilisateur> findById(@PathVariable Long id) {
        return utilisateurRepository.findByIdAndActiveTrue(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email:.+}")
    public ResponseEntity<Utilisateur> findByEmail(@PathVariable String email) {
        return utilisateurRepository.findByEmailAndActiveTrue(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/telephone/{telephone:.+}")
    public ResponseEntity<Utilisateur> findByTelephone(@PathVariable String telephone) {
        return utilisateurRepository.findByTelephoneAndActiveTrue(telephone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<Utilisateur> update(@PathVariable Long id, @RequestBody Utilisateur incoming) {
        return utilisateurRepository.findByIdAndActiveTrue(id)
                .map(existing -> {
                    existing.setNom(incoming.getNom());
                    existing.setPrenom(incoming.getPrenom());
                    existing.setEmail(incoming.getEmail());
                    existing.setTelephone(incoming.getTelephone());
                    existing.setAdresse(incoming.getAdresse());
                    if (incoming.getMotDePasse() != null && !incoming.getMotDePasse().isEmpty()) {
                        existing.setMotDePasse(incoming.getMotDePasse());
                    }
                    if (incoming.getRole() != null) existing.setRole(incoming.getRole());
                    return ResponseEntity.ok(utilisateurRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Désactivation (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(user -> {
                    user.setActive(false);
                    utilisateurRepository.save(user);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Réactivation
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Utilisateur> reactivate(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(user -> {
                    user.setActive(true);
                    return ResponseEntity.ok(utilisateurRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Upgrade vers SHOP_OWNER (appelé lors de l'onboarding boutique)
    @PatchMapping("/{id}/upgrade-to-shop")
    public ResponseEntity<Utilisateur> upgradeToShop(@PathVariable Long id) {
        return utilisateurRepository.findByIdAndActiveTrue(id)
                .map(user -> {
                    user.setSellerType(SellerType.SHOP_OWNER);
                    return ResponseEntity.ok(utilisateurRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
