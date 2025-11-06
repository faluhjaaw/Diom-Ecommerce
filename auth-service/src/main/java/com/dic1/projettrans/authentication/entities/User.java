package com.dic1.projettrans.authentication.entities;

import com.dic1.projettrans.authentication.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String nom;
    private String prenom;
    @Indexed(unique = true)
    private String email;
    private String motDePasse;
    private LocalDateTime dateInscription;
    private Boolean emailConfirme = Boolean.FALSE;
    private Role role;
    private boolean locked = false;
}
