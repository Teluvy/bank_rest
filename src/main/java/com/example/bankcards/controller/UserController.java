package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.PaginatedResponse;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardService;
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

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Операции пользователя со своими картами")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final CardService cardService;
    private final TransferService transferService;

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

    @PostMapping("/cards/{cardId}/block")
    @Operation(summary = "Запросить блокировку своей карты")
    public ResponseEntity<CardResponse> blockMyCard(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long cardId) {
        User user = getUserFromPrincipal(currentUser);
        CardResponse response = cardService.requestBlockByUser(user, cardId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cards/{cardId}/balance")
    @Operation(summary = "Посмотреть баланс карты")
    public ResponseEntity<BigDecimal> getBalance(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long cardId) {
        User user = getUserFromPrincipal(currentUser);
        return ResponseEntity.ok(cardService.getBalance(user, cardId));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Перевод между своими картами")
    public ResponseEntity<Void> transfer(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransferRequest request) {
        User user = getUserFromPrincipal(currentUser);
        transferService.transferBetweenOwnCards(user, request);
        return ResponseEntity.ok().build();
    }

    private User getUserFromPrincipal(UserPrincipal principal) {
        // В реальном коде можно загружать из БД или использовать principal напрямую.
        // Для простоты создадим объект User с id и email, но для полной информации лучше загрузить.
        // Однако сервисы используют репозиторий, поэтому можно передать только id.
        // В CardService метод requestBlockByUser принимает User, но использует только id и email.
        // Сделаем заглушку:
        return User.builder().id(principal.getId()).email(principal.getEmail()).build();
    }
}