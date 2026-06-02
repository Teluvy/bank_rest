package com.example.bankcards.util;

import org.springframework.stereotype.Component;

@Component
public class MaskingUtil {

    public static String maskCardNumber(String fullNumber) {
        if (fullNumber == null || fullNumber.length() < 16) {
            return "**** **** **** ****";
        }
        String last4 = fullNumber.substring(fullNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}