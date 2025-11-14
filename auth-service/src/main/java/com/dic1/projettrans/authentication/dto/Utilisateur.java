package com.dic1.projettrans.authentication.dto;

import com.dic1.projettrans.authentication.enums.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Utilisateur {
    private String prenom;
    private String nom;
    private String email;
    private String telephone;
    private String motDePasse;
    private String adresse;
    private Role role;
}
