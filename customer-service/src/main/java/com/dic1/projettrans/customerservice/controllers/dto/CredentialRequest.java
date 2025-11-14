package com.dic1.projettrans.customerservice.controllers.dto;

import lombok.Data;

@Data
public class CredentialRequest {
    private String email;
    private String password;
}
