package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardOperations;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserOperations;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для административных операций.
 * <p>Доступен только пользователям с ролью ADMIN. Предоставляет возможности:
 * <ul>
 *     <li>CRUD операций с картами</li>
 *     <li>Управление пользователями (создание, удаление, включение/отключение)</li>
 *     <li>Просмотр всех карт с фильтрацией и пагинацией</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Управление картами и пользователями (только ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final CardOperations cardService;
    private final UserOperations userService;

    /**
     * Создаёт новую карту.
     *
     * @param request DTO с данными карты (номер, срок, баланс, ownerId)
     * @return созданная карта (DTO) со статусом 201 CREATED
     */
    @PostMapping("/cards")
    @Operation(summary = "Создать новую карту")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.toResponse(cardService.createCard(request)));
    }

    /**
     * Возвращает список всех карт с поддержкой пагинации и фильтрации
     *
     * @param status   (опционально) фильтр по статусу карты
     * @param ownerId  (опционально) фильтр по владельцу
     * @param page     номер страницы (начиная с 0)
     * @param size     размер страницы
     * @param sortBy   поле для сортировки
     * @param direction направление сортировки (asc/desc)
     * @return страница с DTO карт
     */
    @GetMapping("/cards")
    @Operation(summary = "Просмотр всех карт (с фильтрацией и пагинацией)")
    public ResponseEntity<PaginatedResponse<CardResponse>> getAllCards(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(direction), sortBy);
        Page<CardResponse> pageResult = cardService.getAllCards(status, ownerId, pageable);
        return ResponseEntity.ok(new PaginatedResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        ));
    }

    /**
     * Изменяет статус карты (ACTIVE/BLOCKED)
     *
     * @param cardId  идентификатор карты
     * @param request DTO с новым статусом
     * @return обновлённая карта (DTO)
     */
    @PutMapping("/cards/{cardId}/status")
    @Operation(summary = "Обновить статус карты (активна/заблокирована)")
    public ResponseEntity<CardResponse> updateCardStatus(
            @PathVariable Long cardId,
            @Valid @RequestBody CardStatusUpdateRequest request) {
        return ResponseEntity.ok(cardService.updateCardStatus(cardId, request.getStatus()));
    }

    /**
     * Удаляет карту по идентификатору.
     *
     * @param cardId идентификатор карты
     * @return статус 204 NO_CONTENT
     */
    @DeleteMapping("/cards/{cardId}")
    @Operation(summary = "Удалить карту")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Создаёт нового пользователя (администратором).
     *
     * @param request DTO с email, паролем и ролями
     * @return созданный пользователь (DTO) со статусом 201 CREATED
     */
    @PostMapping("/users")
    @Operation(summary = "Создать пользователя (администратором)")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = userService.createUser(request.getEmail(), request.getPassword(), request.getRoles());
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(user));
    }

    /**
     * Возвращает список всех пользователей с пагинацией.
     *
     * @param page номер страницы
     * @param size размер страницы
     * @return страница с DTO пользователей
     */
    @GetMapping("/users")
    @Operation(summary = "Список всех пользователей (пагинация)")
    public ResponseEntity<PaginatedResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> pageResult = userService.getAllUsers(pageable);
        return ResponseEntity.ok(new PaginatedResponse<>(
                pageResult.map(this::toUserResponse).getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        ));
    }

    /**
     * Удаляет пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     * @return статус 204 NO_CONTENT
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Удалить пользователя")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Включает или отключает пользователя (блокировка учётной записи).
     *
     * @param userId  идентификатор пользователя
     * @param enabled true – включить, false – отключить
     * @return обновлённый пользователь (DTO)
     */
    @PatchMapping("/users/{userId}/enable")
    @Operation(summary = "Включить/отключить пользователя")
    public ResponseEntity<UserResponse> toggleUser(@PathVariable Long userId, @RequestParam boolean enabled) {
        User user = userService.updateUserEnabled(userId, enabled);
        return ResponseEntity.ok(toUserResponse(user));
    }

    /**
     * Преобразует сущность User в DTO UserResponse.
     *
     * @param user сущность пользователя
     * @return DTO пользователя {@link UserResponse}
     */
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toSet()))
                .build();
    }
}