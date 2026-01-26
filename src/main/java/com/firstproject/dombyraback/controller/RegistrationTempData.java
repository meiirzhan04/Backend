package com.firstproject.dombyraback.controller;

import lombok.Data;

@Data
public class RegistrationTempData {
    private String phone;
    private String password;
    private String telegramUsername;
    private String otp;
    private boolean otpVerified = false;
}
