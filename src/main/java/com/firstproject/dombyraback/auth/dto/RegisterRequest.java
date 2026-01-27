package com.firstproject.dombyraback.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String phone;

    @Size(min = 6)
    private String password;

    @Size(min = 6)
    private String confirmPassword;
}