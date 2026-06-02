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

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

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

    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(CardStatus status, Long ownerId, Pageable pageable) {
        return cardRepository.findAllWithFilters(status, ownerId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getUserCards(User user, Pageable pageable) {
        return cardRepository.findByOwner(user, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Card getCardEntity(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));
    }

    @Transactional
    public CardResponse updateCardStatus(Long cardId, CardStatus newStatus) {
        Card card = getCardEntity(cardId);
        card.setStatus(newStatus);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException("Card not found with id: " + cardId);
        }
        cardRepository.deleteById(cardId);
        log.info("Card {} deleted", cardId);
    }

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

    @Transactional(readOnly = true)
    public BigDecimal getBalance(User user, Long cardId) {
        Card card = cardRepository.findByIdAndOwner(cardId, user)
                .orElseThrow(() -> new CardNotFoundException("Card not found or not owned by user"));
        return card.getBalance();
    }

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