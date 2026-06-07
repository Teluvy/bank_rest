package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Реализация операций управления пользователями.
 * <p>Обеспечивает создание, поиск, удаление и изменение статуса пользователей.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserOperations {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Создаёт нового пользователя с указанными ролями.
     * <p>Пароль кодируется с использованием {@link PasswordEncoder} перед сохранением.</p>
     *
     * @param email     электронная почта пользователя (уникальный идентификатор)
     * @param password  пароль в открытом виде (будет закодирован)
     * @param roleNames множество имён ролей (например, {"USER"} или {"ADMIN"})
     * @return сохранённая сущность {@link User}
     * @throws RuntimeException если пользователь с таким email уже существует, либо указанная роль не найдена в БД
     */
    @Transactional
    public User createUser(String email, String password, Set<String> roleNames) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .build();
        Set<Role> roles = roleRepository.findByNameIn(roleNames);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    /**
     * Возвращает страницу со всеми пользователями (с пагинацией).
     *
     * @param pageable параметры пагинации (номер страницы, размер, сортировка)
     * @return страница сущностей {@link User}
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Находит пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя
     * @return найденная сущность {@link User}
     * @throws RuntimeException если пользователь не найден
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Удаляет пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     */
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Включает или отключает пользователя (блокировка учётной записи).
     *
     * @param id      идентификатор пользователя
     * @param enabled true – включить, false – отключить
     * @return обновлённая сущность {@link User}
     * @throws RuntimeException если пользователь не найден
     */
    @Transactional
    public User updateUserEnabled(Long id, boolean enabled) {
        User user = getUserById(id);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }
}