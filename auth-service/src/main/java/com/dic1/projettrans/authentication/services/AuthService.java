package com.dic1.projettrans.authentication.services;

import com.dic1.projettrans.authentication.dto.*;

public interface AuthService {
    void initRegister(String telephone);
    boolean completeRegister(RegisterDTO registerDTO);
    boolean loginUser(LoginDTO loginDTO);
    boolean checkPhone(PhoneCheckDTO phoneCheckDTO);
}
