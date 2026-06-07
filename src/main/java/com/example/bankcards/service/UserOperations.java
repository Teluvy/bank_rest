package com.example.bankcards.service;

import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/**
 * Операции управления пользователями
 */
public interface UserOperations {

    /**
     * Создание нового пользователя
     */
    User createUser(String email, String password, Set<String> roleNames);

    /**
     * Получение всех пользователей с пагинацией
     */
    Page<User> getAllUsers(Pageable pageable);

    /**
     * Удаление пользователя
     */
    void deleteUser(Long userId);

    /**
     * Включение / отключение пользователя
     */
    User updateUserEnabled(Long userId, boolean enabled);
}