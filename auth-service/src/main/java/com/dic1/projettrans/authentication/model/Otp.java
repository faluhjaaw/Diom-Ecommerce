package com.dic1.projettrans.authentication.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Getter
@Setter
@Builder
public class Otp {
    private String email;
    private String otp;
    private String message;
}
