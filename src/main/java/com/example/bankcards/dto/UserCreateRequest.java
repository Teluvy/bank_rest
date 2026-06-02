package com.example.bankcards.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

@Data
public class UserCreateRequest {
    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String password;

    private Set<String> roles;
}