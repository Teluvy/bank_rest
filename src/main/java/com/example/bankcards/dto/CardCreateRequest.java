package com.example.bankcards.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardCreateRequest {
    @NotBlank
    @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
    private String cardNumber;

    @NotNull
    @Future(message = "Expiry date must be in future")
    private LocalDate expiryDate;

    @PositiveOrZero
    private BigDecimal initialBalance;

    @NotNull
    private Long ownerId;
}