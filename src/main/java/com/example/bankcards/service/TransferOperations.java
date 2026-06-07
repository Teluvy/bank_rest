package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.User;

/**
 * Операции перевода средств
 */
public interface TransferOperations {

    /**
     * Перевод средств между своими картами
     */
    void transferBetweenOwnCards(User currentUser, TransferRequest request);
}