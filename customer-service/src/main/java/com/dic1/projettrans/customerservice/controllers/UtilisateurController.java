package com.dic1.projettrans.customerservice.controllers;

import com.dic1.projettrans.customerservice.entities.Role;
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
        Utilisateur saved = utilisateurRepository.save(user);
        return ResponseEntity.created(URI.create("/api/users/" + saved.getId())).body(saved);
    }

    @GetMapping
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Utilisateur> findById(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Utilisateur> update(@PathVariable Long id, @RequestBody Utilisateur incoming) {
        return utilisateurRepository.findById(id)
                .map(existing -> {
                    existing.setNom(incoming.getNom());
                    existing.setEmail(incoming.getEmail());
                    existing.setTelephone(incoming.getTelephone());
                    if (incoming.getMotDePasse() != null && !incoming.getMotDePasse().isEmpty()) {
                        existing.setMotDePasse(incoming.getMotDePasse());
                    }
                    if (incoming.getRole() != null) existing.setRole(incoming.getRole());
                    return ResponseEntity.ok(utilisateurRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!utilisateurRepository.existsById(id)) return ResponseEntity.notFound().build();
        utilisateurRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
