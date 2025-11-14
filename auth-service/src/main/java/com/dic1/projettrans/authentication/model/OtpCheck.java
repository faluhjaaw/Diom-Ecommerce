package com.dic1.projettrans.authentication.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder
public class OtpCheck {
    private boolean valid;
    private String email;
}
