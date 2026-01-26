package com.firstproject.dombyraback.service;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TelegramService {
    private final TelegramBot bot;
    private final Map<Long, String> otpStorage = new HashMap<>();
    private final Map<String, Long> usernameToChat = new HashMap<>();

    public TelegramService(@Value("${telegram.bot.token}") String botToken) {
        this.bot = new TelegramBot(botToken);
        startBot();
    }

    private void startBot() {
        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.message() != null && update.message().text() != null) {
                    Long chatId = update.message().chat().id();
                    String text = update.message().text();

                    if (text.equals("/start")) {
                        String username = update.message().chat().username();
                        if (username != null) {
                            usernameToChat.put(username, chatId);
                            sendMessage(chatId, "Привет! Ваш Telegram подключен к Dombyra. Username: @" + username);
                        } else {
                            sendMessage(chatId, "Пожалуйста, установите username в настройках Telegram");
                        }
                    }
                }
            });
            return com.pengrad.telegrambot.UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public boolean sendOTPByUsername(String username, String otp) {
        Long chatId = usernameToChat.get(username);

        if (chatId == null) {
            return false; // Пользователь не подключил бота
        }

        // Сохраняем OTP на 5 минут
        otpStorage.put(chatId, otp);

        // Удаляем OTP через 5 минут
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                otpStorage.remove(chatId);
            }
        }, 5 * 60 * 1000);

        String message = "🔐 Ваш код подтверждения: " + otp + "\n\nКод действителен 5 минут.";
        return sendMessage(chatId, message);
    }
    public boolean verifyOTP(String username, String otp) {
        Long chatId = usernameToChat.get(username);
        if (chatId == null) {
            return false;
        }

        String storedOTP = otpStorage.get(chatId);
        if (storedOTP != null && storedOTP.equals(otp)) {
            otpStorage.remove(chatId);
            return true;
        }
        return false;
    }

    private boolean sendMessage(Long chatId, String text) {
        try {
            bot.execute(new SendMessage(chatId, text));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean isUserConnected(String username) {
        return usernameToChat.containsKey(username);
    }
}
