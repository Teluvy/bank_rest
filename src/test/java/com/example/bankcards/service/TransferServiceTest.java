package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.TransferNotAllowedException;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private CardService cardService;
    @InjectMocks private TransferService transferService;

    private User user;
    private Card fromCard;
    private Card toCard;
    private final Long fromId = 1L;
    private final Long toId = 2L;

    @BeforeEach
    void setUp() {
        user = User.builder().id(10L).email("user@test.com").build();
        fromCard = Card.builder()
                .id(fromId)
                .balance(BigDecimal.valueOf(1000))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(1))
                .owner(user)
                .build();
        toCard = Card.builder()
                .id(toId)
                .balance(BigDecimal.valueOf(100))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(1))
                .owner(user)
                .build();
    }

    @Test
    void shouldTransferSuccessfully() {
        when(cardRepository.findByIdAndOwner(fromId, user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(toId, user)).thenReturn(Optional.of(toCard));
        doNothing().when(cardService).checkCardActive(any(Card.class));

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromId);
        request.setToCardId(toId);
        request.setAmount(BigDecimal.valueOf(200));

        transferService.transferBetweenOwnCards(user, request);

        assertEquals(BigDecimal.valueOf(800), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(300), toCard.getBalance());
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void shouldThrowWhenFromCardNotFound() {
        when(cardRepository.findByIdAndOwner(fromId, user)).thenReturn(Optional.empty());
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromId);
        request.setToCardId(toId);
        request.setAmount(BigDecimal.TEN);
        assertThrows(TransferNotAllowedException.class,
                () -> transferService.transferBetweenOwnCards(user, request));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenToCardNotFound() {
        when(cardRepository.findByIdAndOwner(fromId, user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(toId, user)).thenReturn(Optional.empty());
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromId);
        request.setToCardId(toId);
        request.setAmount(BigDecimal.TEN);
        assertThrows(TransferNotAllowedException.class,
                () -> transferService.transferBetweenOwnCards(user, request));
    }

    @Test
    void shouldThrowWhenSameCard() {
        when(cardRepository.findByIdAndOwner(fromId, user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(fromId, user)).thenReturn(Optional.of(fromCard));
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromId);
        request.setToCardId(fromId);
        request.setAmount(BigDecimal.TEN);
        assertThrows(TransferNotAllowedException.class,
                () -> transferService.transferBetweenOwnCards(user, request));
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {
        when(cardRepository.findByIdAndOwner(fromId, user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(toId, user)).thenReturn(Optional.of(toCard));
        doNothing().when(cardService).checkCardActive(any(Card.class));

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromId);
        request.setToCardId(toId);
        request.setAmount(BigDecimal.valueOf(2000)); // больше чем 1000

        assertThrows(InsufficientFundsException.class,
                () -> transferService.transferBetweenOwnCards(user, request));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void shouldCallCheckCardActiveForBothCards() {
        when(cardRepository.findByIdAndOwner(fromId, user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(toId, user)).thenReturn(Optional.of(toCard));
        doNothing().when(cardService).checkCardActive(fromCard);
        doNothing().when(cardService).checkCardActive(toCard);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromId);
        request.setToCardId(toId);
        request.setAmount(BigDecimal.valueOf(100));

        transferService.transferBetweenOwnCards(user, request);

        verify(cardService).checkCardActive(fromCard);
        verify(cardService).checkCardActive(toCard);
    }
}