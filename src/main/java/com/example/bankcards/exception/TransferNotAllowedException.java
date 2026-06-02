package com.example.bankcards.exception;

public class TransferNotAllowedException extends RuntimeException {
    public TransferNotAllowedException(String message) {
        super(message);
    }
}