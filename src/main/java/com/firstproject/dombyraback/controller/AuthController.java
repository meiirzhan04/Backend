package com.firstproject.dombyraback.controller;

import com.firstproject.dombyraback.auth.AuthResponse;
import com.firstproject.dombyraback.auth.AuthService;
import com.firstproject.dombyraback.auth.dto.LoginRequest;
import com.firstproject.dombyraback.auth.dto.RegisterRequest;
import com.firstproject.dombyraback.security.JwtService;
import com.firstproject.dombyraback.service.TelegramService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final TelegramService telegramService;
    private final AuthService authService;
    private final JwtService jwtService;

    // Временное хранилище для регистрации (в production используй Redis или БД)
    private final Map<String, RegistrationTempData> registrationSessions = new ConcurrentHashMap<>();

    public AuthController(AuthService authService, TelegramService telegramService, JwtService jwtService) {
        this.authService = authService;
        this.telegramService = telegramService;
        this.jwtService = jwtService;
    }

    // ШАГ 1: Инициализация регистрации (телефон + пароль)
    @PostMapping("/register/init")
    public ResponseEntity<?> initRegistration(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String password = request.get("password");
        String confirmPassword = request.get("confirmPassword");
        String telegramUsername = request.get("telegramUsername");

        // Валидация
        if (phone == null || password == null || confirmPassword == null || telegramUsername == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Все поля обязательны"));
        }

        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Пароли не совпадают"));
        }

        if (!telegramService.isUserConnected(telegramUsername)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Пожалуйста, сначала откройте бота и нажмите /start",
                    "botUsername", "dombyra_auth_bot"
            ));
        }

        // Сохраняем временные данные
        RegistrationTempData tempData = new RegistrationTempData();
        tempData.setPhone(phone);
        tempData.setPassword(password); // В production хэшируй!
        tempData.setTelegramUsername(telegramUsername);

        registrationSessions.put(telegramUsername, tempData);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Данные приняты. Отправляем OTP код...",
                "nextStep", "verify_otp"
        ));
    }

    // ШАГ 2: Отправка OTP
    @PostMapping("/register/request-otp")
    public ResponseEntity<?> requestRegistrationOTP(@RequestBody Map<String, String> request) {
        String telegramUsername = request.get("telegramUsername");

        if (telegramUsername == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Telegram username required"));
        }

        RegistrationTempData tempData = registrationSessions.get(telegramUsername);
        if (tempData == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Сессия не найдена. Начните регистрацию заново."));
        }

        String otp = telegramService.generateOTP();
        boolean sent = telegramService.sendOTPByUsername(telegramUsername, otp);

        if (sent) {
            tempData.setOtp(otp);
            registrationSessions.put(telegramUsername, tempData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP отправлен в Telegram",
                    "nextStep", "enter_name"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Не удалось отправить OTP"));
        }
    }

    // ШАГ 3: Верификация OTP
    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyRegistrationOTP(@RequestBody Map<String, String> request) {
        String telegramUsername = request.get("telegramUsername");
        String otp = request.get("otp");

        if (telegramUsername == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and OTP required"));
        }

        RegistrationTempData tempData = registrationSessions.get(telegramUsername);
        if (tempData == null || tempData.getOtp() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP не запрошен"));
        }

        if (tempData.getOtp().equals(otp)) {
            tempData.setOtpVerified(true);
            registrationSessions.put(telegramUsername, tempData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP подтвержден!",
                    "nextStep", "complete_registration"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Неверный OTP"
            ));
        }
    }

    // ШАГ 4: Завершение регистрации (ввод имени)
    @PostMapping("/register/complete")
    public ResponseEntity<?> completeRegistration(@RequestBody Map<String, String> request) {
        String telegramUsername = request.get("telegramUsername");
        String name = request.get("name"); // Получаем имя

        if (telegramUsername == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Обязательные поля: telegramUsername, name"));
        }

        RegistrationTempData tempData = registrationSessions.get(telegramUsername);
        if (tempData == null || !tempData.isOtpVerified()) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP не подтвержден"));
        }

        // Создаем RegisterRequest и заполняем данными
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhone(tempData.getPhone());
        registerRequest.setPassword(tempData.getPassword());
        registerRequest.setName(name); // <- Теперь используем setName()
        registerRequest.setTelegramUsername(telegramUsername);

        AuthResponse authResponse = authService.register(registerRequest);

        // Генерируем JWT токен
        String token = jwtService.generateToken(telegramUsername);

        // Удаляем временную сессию
        registrationSessions.remove(telegramUsername);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Регистрация завершена!",
                "token", token,
                "tokenType", "Bearer",
                "user", authResponse.getUser()
        ));
    }


    // Существующие методы остаются без изменений
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
            String token = jwtService.generateToken(username);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP подтвержден успешно!",
                    "token", token
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
                "botUsername", "DombyraAppAuth"
        ));
    }
}

