package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.LoginDTO;

public interface TokenService {
    String generateToken(LoginDTO loginDTO);
}