package com.example.bankcards.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        // Ключ должен быть 32 байта в строке (для теста)
        encryptionUtil = new EncryptionUtil("aesEncryptionKey1234567890123456");
    }

    @Test
    void shouldEncryptAndDecrypt() {
        String original = "1234567890123456";
        String encrypted = encryptionUtil.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void shouldProduceDifferentEncryptionsForSameInput() {
        String original = "1234567890123456";
        String encrypted1 = encryptionUtil.encrypt(original);
        String encrypted2 = encryptionUtil.encrypt(original);
        assertNotEquals(encrypted1, encrypted2);
    }
}