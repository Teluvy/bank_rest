package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Основные операции с банковскими картами
 */
public interface CardOperations {

    /**
     * Создаёт новую карту (только для администратора)
     */
    Card createCard(CardCreateRequest request);

    /**
     * Возвращает карту по её ID
     */
    Card getCardEntity(Long cardId);

    /**
     * Обновляет статус карты (блокировка/активация)
     */
    CardResponse updateCardStatus(Long cardId, CardStatus newStatus);

    /**
     * Удаляет карту
     */
    void deleteCard(Long cardId);

    /**
     * Пользователь запрашивает блокировку своей карты
     */
    CardResponse requestBlockByUser(User user, Long cardId);

    /**
     * Получить баланс карты пользователя
     */
    BigDecimal getBalance(User user, Long cardId);

    /**
     * Просмотр карт пользователя с пагинацией и фильтрацией по статусу
     */
    Page<CardResponse> getUserCardsWithStatus(User user, CardStatus status, Pageable pageable);

    /**
     * Просмотр всех карт с фильтрацией
     */
    Page<CardResponse> getAllCards(CardStatus status, Long ownerId, Pageable pageable);

    /**
     * Преобразует сущность {@link Card} в DTO {@link CardResponse}
     *
     * @param card
     * @return {@link CardResponse}
     */
    CardResponse toResponse(Card card);
}