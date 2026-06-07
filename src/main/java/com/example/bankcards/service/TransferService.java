package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.TransferNotAllowedException;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService implements TransferOperations {

    private final CardRepository cardRepository;
    private final CardService cardService;

    @Transactional
    public void transferBetweenOwnCards(User currentUser, TransferRequest request) {
        Card fromCard = cardRepository.findByIdAndOwner(request.getFromCardId(), currentUser)
                .orElseThrow(() -> new TransferNotAllowedException("From card not found or not owned by user"));
        Card toCard = cardRepository.findByIdAndOwner(request.getToCardId(), currentUser)
                .orElseThrow(() -> new TransferNotAllowedException("To card not found or not owned by user"));

        cardService.checkCardActive(fromCard);
        cardService.checkCardActive(toCard);

        if (fromCard.getId().equals(toCard.getId())) {
            throw new TransferNotAllowedException("Cannot transfer to the same card");
        }

        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        log.info("Transfer {} from card {} to card {} by user {}", request.getAmount(),
                fromCard.getId(), toCard.getId(), currentUser.getEmail());
    }
}