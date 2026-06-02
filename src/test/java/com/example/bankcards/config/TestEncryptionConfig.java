package com.example.bankcards.config;

import com.example.bankcards.util.EncryptionUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@TestConfiguration
public class TestEncryptionConfig {

    @Bean
    @Primary
    public EncryptionUtil testEncryptionUtil() {
        // фиктивный ключ для тестов
        String key = "aesEncryptionKey1234567890123456";
        return new EncryptionUtil(key);
    }
}