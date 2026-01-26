package com.firstproject.dombyraback.service;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationSession {
    @Id
    private String telegramUsername;

    private String phone;
    private String hashedPassword;
    private String tempOtp;
    private LocalDateTime otpCreatedAt;
    private RegistrationStep step; // INIT, OTP_SENT, NAME_PENDING
    private LocalDateTime createdAt;
}
