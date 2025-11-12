package com.dic1.projettrans.authentication.model;

import com.dic1.projettrans.authentication.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUser {
    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String motDePasse;
    private String adresse;
    private Role role;
}