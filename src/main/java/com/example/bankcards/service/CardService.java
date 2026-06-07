package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.CardExpiredException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.EncryptionUtil;
import com.example.bankcards.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Сервис для управления банковскими картами.
 * Предоставляет методы для создания, поиска, блокировки, удаления карт,
 * а также проверки их статуса и срока действия.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardService implements CardOperations {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * Создаёт новую карту для указанного пользователя.
     * Номер карты шифруется с использованием {@link EncryptionUtil}.
     *
     * @param request DTO с данными для создания карты (номер, срок действия, баланс, владелец)
     * @return созданная сущность Card
     * @throws RuntimeException если владелец не найден
     */
    @Transactional
    public Card createCard(CardCreateRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        String encryptedNumber = encryptionUtil.encrypt(request.getCardNumber());
        String maskedNumber = MaskingUtil.maskCardNumber(request.getCardNumber());

        Card card = Card.builder()
                .encryptedNumber(encryptedNumber)
                .maskedNumber(maskedNumber)
                .owner(owner)
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .build();

        return cardRepository.save(card);
    }

    /**
     * Возвращает страницу всех карт с возможностью фильтрации по статусу и владельцу
     *
     * @param status   (опционально) фильтр по статусу карты (ACTIVE, BLOCKED, EXPIRED)
     * @param ownerId  (опционально) фильтр по идентификатору владельца
     * @param pageable параметры пагинации и сортировки
     * @return страница DTO {@link CardResponse}
     */
    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(CardStatus status, Long ownerId, Pageable pageable) {
        return cardRepository.findAllWithFilters(status, ownerId, pageable)
                .map(this::toResponse);
    }

    /**
     * Возвращает страницу карт, принадлежащих конкретному пользователю (без фильтра по статусу).
     *
     * @param user     владелец карт
     * @param pageable параметры пагинации
     * @return страница DTO {@link CardResponse}
     */
    @Transactional(readOnly = true)
    public Page<CardResponse> getUserCards(User user, Pageable pageable) {
        return cardRepository.findByOwner(user, pageable)
                .map(this::toResponse);
    }

    /**
     * Возвращает сущность карты по идентификатору (для внутренних проверок).
     *
     * @param cardId идентификатор карты
     * @return сущность {@link Card}
     * @throws CardNotFoundException если карта не найдена
     */
    @Transactional(readOnly = true)
    public Card getCardEntity(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));
    }

    /**
     * Изменяет статус карты (ACTIVE, BLOCKED, EXPIRED)
     *
     * @param cardId    идентификатор карты
     * @param newStatus новый статус
     * @return DTO {@link CardResponse} с обновлёнными данными
     * @throws CardNotFoundException если карта не найдена
     */
    @Transactional
    public CardResponse updateCardStatus(Long cardId, CardStatus newStatus) {
        Card card = getCardEntity(cardId);
        card.setStatus(newStatus);
        return toResponse(cardRepository.save(card));
    }

    /**
     * Удаляет карту по идентификатору.
     *
     * @param cardId идентификатор карты
     * @throws CardNotFoundException если карта не найдена
     */
    @Transactional
    public void deleteCard(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException("Card not found with id: " + cardId);
        }
        cardRepository.deleteById(cardId);
        log.info("Card {} deleted", cardId);
    }

    /**
     * Обрабатывает запрос пользователя на блокировку своей карты.
     * <p>Карта переводится в статус BLOCKED, если она не просрочена.</p>
     *
     * @param user   пользователь, инициатор запроса
     * @param cardId идентификатор карты
     * @return DTO {@link CardResponse} с обновлённым статусом
     * @throws CardNotFoundException если карта не найдена или не принадлежит пользователю
     * @throws RuntimeException      если карта уже просрочена
     */
    @Transactional
    public CardResponse requestBlockByUser(User user, Long cardId) {
        Card card = cardRepository.findByIdAndOwner(cardId, user)
                .orElseThrow(() -> new CardNotFoundException("Card not found or not owned by user"));
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new RuntimeException("Cannot block expired card");
        }
        card.setStatus(CardStatus.BLOCKED);
        return toResponse(cardRepository.save(card));
    }

    /**
     * Возвращает баланс карты, принадлежащей пользователю
     *
     * @param user   владелец карты
     * @param cardId идентификатор карты
     * @return текущий баланс
     * @throws CardNotFoundException если карта не найдена или не принадлежит пользователю
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(User user, Long cardId) {
        Card card = cardRepository.findByIdAndOwner(cardId, user)
                .orElseThrow(() -> new CardNotFoundException("Card not found or not owned by user"));
        return card.getBalance();
    }

    /**
     * Проверяет, активна ли карта и не истёк ли её срок действия.
     * <p>Если срок истёк, карта автоматически переводится в статус EXPIRED и выбрасывается исключение.</p>
     *
     * @param card карта для проверки
     * @throws CardExpiredException если срок действия истёк
     * @throws CardBlockedException если карта заблокирована
     */
    void checkCardActive(Card card) {
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
            throw new CardExpiredException("Card is expired");
        }
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardBlockedException("Card is " + card.getStatus().name().toLowerCase());
        }
    }

    /**
     * Преобразует сущность {@link Card} в DTO {@link CardResponse} для передачи клиенту
     *
     * @param card сущность карты
     * @return DTO с маскированным номером, статусом, балансом и другой информацией
     */
    public CardResponse toResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .ownerEmail(card.getOwner().getEmail())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .build();
    }

    /**
     * Возвращает страницу карт пользователя с фильтрацией по статусу.
     *
     * @param user     владелец карт
     * @param status   фильтр по статусу (ACTIVE, BLOCKED, EXPIRED)
     * @param pageable параметры пагинации
     * @return страница DTO {@link CardResponse}
     */
    public Page<CardResponse> getUserCardsWithStatus(User user, CardStatus status, Pageable pageable) {
        Page<Card> cards;
        if (status != null) {
            cards = cardRepository.findByOwnerAndStatus(user, status, pageable);
        } else {
            cards = cardRepository.findByOwner(user, pageable);
        }
        return cards.map(this::toResponse);
    }
}