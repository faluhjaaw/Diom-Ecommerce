package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.LoginDTO;
import com.dic1.projettrans.authentication.dto.MailCheckDTO;
import com.dic1.projettrans.authentication.dto.RegisterDTO;

public interface AuthService {
    void initRegister(RegisterDTO registerDTO);
    boolean completeRegister(RegisterDTO registerDTO, String otpCode);
    boolean loginUser(LoginDTO loginDTO);
}
