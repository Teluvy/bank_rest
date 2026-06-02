package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private Set<String> roles;
}