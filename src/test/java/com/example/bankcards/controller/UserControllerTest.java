package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CardRepository cardRepository;

    private Long testCardId;
    private Long secondCardId;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .email("testuser@example.com")
                .password("dummy")
                .enabled(true)
                .build());

        Card card1 = cardRepository.save(Card.builder()
                .encryptedNumber("enc1")
                .maskedNumber("**** **** **** 1111")
                .owner(user)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build());
        testCardId = card1.getId();

        Card card2 = cardRepository.save(Card.builder()
                .encryptedNumber("enc2")
                .maskedNumber("**** **** **** 2222")
                .owner(user)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(100))
                .build());
        secondCardId = card2.getId();

        // Создаём UserPrincipal и кладём в SecurityContextHolder
        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                "USER",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldGetUserCards() throws Exception {
        mockMvc.perform(get("/api/user/cards")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testCardId));
    }

    @Test
    void shouldBlockCard() throws Exception {
        mockMvc.perform(post("/api/user/cards/" + testCardId + "/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void shouldGetBalance() throws Exception {
        mockMvc.perform(get("/api/user/cards/" + testCardId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string("1000"));
    }

    @Test
    void shouldTransfer() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(testCardId);
        request.setToCardId(secondCardId);
        request.setAmount(BigDecimal.valueOf(200));

        mockMvc.perform(post("/api/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}