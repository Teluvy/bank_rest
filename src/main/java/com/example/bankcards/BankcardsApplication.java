package com.example.bankcards;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(title = "Bank Cards API", version = "1.0", description = "Управление банковскими картами"),
        security = @SecurityRequirement(name = "bearerAuth")
)
public class BankcardsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankcardsApplication.class, args);
    }
}