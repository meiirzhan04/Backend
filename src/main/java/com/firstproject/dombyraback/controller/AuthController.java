package com.firstproject.dombyraback.controller;

import com.firstproject.dombyraback.auth.AuthResponse;
import com.firstproject.dombyraback.auth.AuthService;
import com.firstproject.dombyraback.auth.dto.LoginRequest;
import com.firstproject.dombyraback.auth.dto.RegisterRequest;
import com.firstproject.dombyraback.service.TelegramService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final TelegramService telegramService;
    private final AuthService authService;

    public AuthController(AuthService authService, TelegramService telegramService) {
        this.authService = authService;
        this.telegramService = telegramService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOTP(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        if (!telegramService.isUserConnected(username)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Пожалуйста, сначала откройте бота и нажмите /start",
                    "botUsername", "dombyra_auth_bot"
            ));
        }

        String otp = telegramService.generateOTP();
        boolean sent = telegramService.sendOTPByUsername(username, otp);

        if (sent) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP отправлен в Telegram"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Не удалось отправить OTP"));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String otp = request.get("otp");

        if (username == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and OTP are required"));
        }

        boolean isValid = telegramService.verifyOTP(username, otp);

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP подтвержден успешно!",
                    "token", "jwt_token_here" // TODO: Создать настоящий JWT
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Неверный или истекший OTP"
            ));
        }
    }

    @GetMapping("/check-telegram/{username}")
    public ResponseEntity<?> checkTelegram(@PathVariable String username) {
        boolean connected = telegramService.isUserConnected(username);
        return ResponseEntity.ok(Map.of(
                "connected", connected,
                "botUsername", "DombyraAuthBot"
        ));
    }
}