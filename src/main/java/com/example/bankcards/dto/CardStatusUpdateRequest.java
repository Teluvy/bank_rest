package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CardStatusUpdateRequest {
    @NotNull
    private CardStatus status;
}