package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.EncryptionUtil;
import com.example.bankcards.util.MaskingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private EncryptionUtil encryptionUtil;
    @InjectMocks private CardService cardService;

    private User testUser;
    private Card testCard;
    private final Long userId = 1L;
    private final Long cardId = 100L;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(userId).email("user@example.com").build();
        testCard = Card.builder()
                .id(cardId)
                .encryptedNumber("encrypted")
                .maskedNumber("**** **** **** 1234")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
    }

    @Test
    void shouldCreateCard() {
        CardCreateRequest request = new CardCreateRequest();
        request.setCardNumber("1234567890123456");
        request.setExpiryDate(LocalDate.now().plusYears(3));
        request.setInitialBalance(BigDecimal.valueOf(200));
        request.setOwnerId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(encryptionUtil.encrypt(anyString())).thenReturn("encryptedNumber");
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card created = cardService.createCard(request);

        assertNotNull(created);
        assertEquals("encryptedNumber", created.getEncryptedNumber());
        assertEquals("**** **** **** 3456", created.getMaskedNumber()); // MaskingUtil.maskCardNumber
        assertEquals(CardStatus.ACTIVE, created.getStatus());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(created.getBalance()));

        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void shouldThrowWhenUserNotFoundOnCreate() {
        CardCreateRequest request = new CardCreateRequest();
        request.setOwnerId(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cardService.createCard(request));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void shouldBlockCardByUser() {
        when(cardRepository.findByIdAndOwner(cardId, testUser)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse response = cardService.requestBlockByUser(testUser, cardId);

        assertEquals(CardStatus.BLOCKED, testCard.getStatus());
        verify(cardRepository).save(testCard);
    }

    @Test
    void shouldThrowWhenBlockingNonOwnedCard() {
        when(cardRepository.findByIdAndOwner(eq(cardId), any(User.class))).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardService.requestBlockByUser(testUser, cardId));
    }

    @Test
    void shouldGetBalance() {
        when(cardRepository.findByIdAndOwner(cardId, testUser)).thenReturn(Optional.of(testCard));

        BigDecimal balance = cardService.getBalance(testUser, cardId);
        assertEquals(BigDecimal.valueOf(500), balance);
    }

    @Test
    void shouldUpdateCardStatus() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse response = cardService.updateCardStatus(cardId, CardStatus.BLOCKED);
        assertEquals(CardStatus.BLOCKED, response.getStatus());
    }

    @Test
    void shouldDeleteCard() {
        when(cardRepository.existsById(cardId)).thenReturn(true);
        doNothing().when(cardRepository).deleteById(cardId);

        assertDoesNotThrow(() -> cardService.deleteCard(cardId));
        verify(cardRepository).deleteById(cardId);
    }

    @Test
    void shouldThrowWhenDeletingNonExistingCard() {
        when(cardRepository.existsById(cardId)).thenReturn(false);
        assertThrows(CardNotFoundException.class, () -> cardService.deleteCard(cardId));
    }

    @Test
    void shouldCheckActiveCard_Expired() {
        testCard.setExpiryDate(LocalDate.now().minusDays(1));
        testCard.setStatus(CardStatus.ACTIVE);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        assertThrows(RuntimeException.class, () -> cardService.checkCardActive(testCard));
        assertEquals(CardStatus.EXPIRED, testCard.getStatus());
        verify(cardRepository).save(testCard);
    }
}