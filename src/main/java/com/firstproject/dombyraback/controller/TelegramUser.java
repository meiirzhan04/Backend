package com.firstproject.dombyraback.controller;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private Long chatId;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "last_otp")
    private String lastOtp;

    @Column(name = "otp_created_at")
    private LocalDateTime otpCreatedAt;
}