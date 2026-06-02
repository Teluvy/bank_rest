package com.example.bankcards.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaskingUtilTest {

    @Test
    void shouldMaskCardNumber() {
        String cardNumber = "1234567890123456";
        String masked = MaskingUtil.maskCardNumber(cardNumber);
        assertEquals("**** **** **** 3456", masked);
    }

    @Test
    void shouldReturnPlaceholderForInvalidLength() {
        assertEquals("**** **** **** ****", MaskingUtil.maskCardNumber(null));
        assertEquals("**** **** **** ****", MaskingUtil.maskCardNumber("123"));
    }
}