package com.firstproject.dombyraback.repository;

import com.firstproject.dombyraback.controller.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

    Optional<TelegramUser> findByUsername(String username);

    Optional<TelegramUser> findByChatId(Long chatId);
}