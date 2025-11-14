package com.dic1.projettrans.customerservice.controllers.dto;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String email;
    private String code;
}
