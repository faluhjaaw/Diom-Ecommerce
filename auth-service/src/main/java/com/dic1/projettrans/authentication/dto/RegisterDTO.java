package com.dic1.projettrans.authentication.dto;

import com.dic1.projettrans.authentication.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO {
    private String prenom;
    private String nom;
    private String email;
    private String password;
    private Role role;
}
