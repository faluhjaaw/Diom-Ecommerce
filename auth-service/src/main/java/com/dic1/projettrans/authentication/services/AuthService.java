package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.*;

public interface AuthService {
    void initRegister(OtpGenerateDTO registerDTO);
    boolean completeRegister(RegisterDTO registerDTO);
    boolean loginUser(LoginDTO loginDTO);
    boolean checkMail(MailCheckDTO mailCheckDTO);
}
