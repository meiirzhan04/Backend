package com.firstproject.dombyraback.service;


import com.firstproject.dombyraback.controller.TelegramUser;
import com.firstproject.dombyraback.repository.TelegramUserRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;


@Service
public class TelegramService {

    private TelegramBot bot;

    @Autowired
    private TelegramUserRepository userRepository;

    @Value("${telegram.bot.token}")
    private String botToken;

    @PostConstruct
    public void init() {
        System.out.println("🤖 Инициализация Telegram бота...");

        try {
            this.bot = new TelegramBot(botToken);
            startBot();
            System.out.println("✅ Telegram бот успешно запущен!");

            // Показать подключенных пользователей
            long count = userRepository.count();
            System.out.println("📊 Подключенных пользователей в БД: " + count);
        } catch (Exception e) {
            System.err.println("❌ Ошибка запуска бота: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startBot() {
        bot.setUpdatesListener(updates -> {
            System.out.println("📨 Получено обновлений: " + updates.size());

            for (Update update : updates) {
                if (update.message() != null && update.message().text() != null) {
                    Long chatId = update.message().chat().id();
                    String text = update.message().text();
                    String username = update.message().chat().username();
                    String firstName = update.message().chat().firstName();

                    System.out.println("💬 Сообщение от: " + firstName + " (@" + username + ")");

                    if (text.equals("/start")) {
                        if (username != null) {
                            // Сохраняем в БД
                            Optional<TelegramUser> existingUser = userRepository.findByUsername(username);

                            TelegramUser user;
                            if (existingUser.isPresent()) {
                                user = existingUser.get();
                                user.setChatId(chatId);
                                user.setConnectedAt(LocalDateTime.now());
                                System.out.println("🔄 Обновление пользователя @" + username);
                            } else {
                                user = new TelegramUser();
                                user.setUsername(username);
                                user.setChatId(chatId);
                                user.setConnectedAt(LocalDateTime.now());
                                System.out.println("➕ Новый пользователь @" + username);
                            }

                            userRepository.save(user);

                            String welcomeMessage = "👋 Привет, " + firstName + "!\n\n" +
                                    "✅ Ваш Telegram подключен к Dombyra\n" +
                                    "📱 Username: @" + username + "\n" +
                                    "🔑 Chat ID: " + chatId + "\n\n" +
                                    "Теперь вы можете получать OTP коды!";
                            sendMessage(chatId, welcomeMessage);

                            System.out.println("✅ Пользователь @" + username + " сохранен в БД!");
                        } else {
                            sendMessage(chatId, "⚠️ Пожалуйста, установите username в настройках Telegram\n\n" +
                                    "Settings → Edit Profile → Username");
                        }
                    }
                }
            }

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            System.err.println("❌ Ошибка при получении обновлений: " + e.getMessage());
        });
    }

    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public boolean sendOTPByUsername(String username, String otp) {
        System.out.println("🔍 Поиск пользователя: @" + username);

        Optional<TelegramUser> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            System.out.println("❌ Пользователь @" + username + " не найден в БД!");
            return false;
        }

        TelegramUser user = userOpt.get();
        Long chatId = user.getChatId();

        System.out.println("✅ Пользователь найден! Chat ID: " + chatId);

        // Сохраняем OTP в БД
        user.setLastOtp(otp);
        user.setOtpCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        String message = "🔐 Ваш код подтверждения: " + otp + "\n\n" +
                "⏱️ Код действителен 5 минут\n" +
                "⚠️ Не сообщайте этот код никому!";

        return sendMessage(chatId, message);
    }

    public boolean verifyOTP(String username, String otp) {
        System.out.println("🔍 Проверка OTP для @" + username);

        Optional<TelegramUser> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            System.out.println("❌ Пользователь не найден");
            return false;
        }

        TelegramUser user = userOpt.get();
        String storedOTP = user.getLastOtp();
        LocalDateTime otpCreatedAt = user.getOtpCreatedAt();

        if (storedOTP == null || otpCreatedAt == null) {
            System.out.println("❌ OTP не был запрошен");
            return false;
        }

        // Проверяем не истек ли OTP (5 минут)
        LocalDateTime now = LocalDateTime.now();
        if (otpCreatedAt.plusMinutes(5).isBefore(now)) {
            System.out.println("❌ OTP истек");
            return false;
        }

        if (storedOTP.equals(otp)) {
            // Удаляем использованный OTP
            user.setLastOtp(null);
            user.setOtpCreatedAt(null);
            userRepository.save(user);

            System.out.println("✅ OTP верный!");
            return true;
        }

        System.out.println("❌ OTP неверный");
        return false;
    }

    private boolean sendMessage(Long chatId, String text) {
        try {
            SendResponse response = bot.execute(new SendMessage(chatId, text));
            if (response.isOk()) {
                System.out.println("✅ Сообщение отправлено!");
                return true;
            } else {
                System.out.println("❌ Ошибка отправки: " + response.description());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Исключение при отправке: " + e.getMessage());
            return false;
        }
    }

    public boolean isUserConnected(String username) {
        boolean connected = userRepository.findByUsername(username).isPresent();
        System.out.println("🔍 Проверка подключения @" + username + ": " + connected);
        return connected;
    }
}


enum RegistrationStep {
    INIT, OTP_SENT, NAME_PENDING, COMPLETED
}