package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.PaginatedResponse;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardOperations;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferOperations;
import com.example.bankcards.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Контроллер для операций пользователя со своими картами.
 * Доступен только для аутентифицированных пользователей с ролью USER.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Операции пользователя со своими картами")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final CardOperations cardService;
    private final TransferOperations transferService;

    /**
     * Возвращает список карт текущего пользователя с пагинацией и возможностью фильтрации по статусу.
     *
     * @param currentUser аутентифицированный пользователь (извлекается из токена)
     * @param status      (опционально) фильтр по статусу карты
     * @param page        номер страницы
     * @param size        размер страницы
     * @param sortBy      поле для сортировки
     * @param direction   направление сортировки
     * @return страница с DTO карт
     */
    @GetMapping("/cards")
    @Operation(summary = "Просмотр своих карт (пагинация, фильтр по статусу)")
    public ResponseEntity<PaginatedResponse<CardResponse>> getMyCards(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(direction), sortBy);
        User user = getUserFromPrincipal(currentUser);
        Page<CardResponse> pageResult = cardService.getUserCardsWithStatus(user, status, pageable);
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
     * Запрашивает блокировку собственной карты (статус меняется на BLOCKED).
     *
     * @param currentUser аутентифицированный пользователь
     * @param cardId      идентификатор карты
     * @return обновлённая карта (DTO) с новым статусом
     */
    @PostMapping("/cards/{cardId}/block")
    @Operation(summary = "Запросить блокировку своей карты")
    public ResponseEntity<CardResponse> blockMyCard(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long cardId) {
        User user = getUserFromPrincipal(currentUser);
        CardResponse response = cardService.requestBlockByUser(user, cardId);
        return ResponseEntity.ok(response);
    }

    /**
     * Возвращает баланс указанной карты.
     *
     * @param currentUser аутентифицированный пользователь
     * @param cardId      идентификатор карты
     * @return текущий баланс в виде числа
     */
    @GetMapping("/cards/{cardId}/balance")
    @Operation(summary = "Посмотреть баланс карты")
    public ResponseEntity<BigDecimal> getBalance(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long cardId) {
        User user = getUserFromPrincipal(currentUser);
        return ResponseEntity.ok(cardService.getBalance(user, cardId));
    }

    /**
     * Выполняет перевод средств между двумя картами текущего пользователя.
     *
     * @param currentUser аутентифицированный пользователь
     * @param request     DTO с идентификаторами карт и суммой перевода
     * @return статус 200 OK при успешном переводе
     */
    @PostMapping("/transfer")
    @Operation(summary = "Перевод между своими картами")
    public ResponseEntity<Void> transfer(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransferRequest request) {
        User user = getUserFromPrincipal(currentUser);
        transferService.transferBetweenOwnCards(user, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Вспомогательный метод: преобразует UserPrincipal в упрощённый объект User.
     *
     * @param principal объект аутентификации
     * @return объект User с заполненными id и email
     */
    private User getUserFromPrincipal(UserPrincipal principal) {
        return User.builder().id(principal.getId()).email(principal.getEmail()).build();
    }
}