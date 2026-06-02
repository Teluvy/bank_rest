package com.example.bankcards.repository;

import com.example.bankcards.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CardRepositoryTest {

    @Autowired private CardRepository cardRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    @Test
    void shouldFindCardsByOwner() {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow();
        User owner = userRepository.save(User.builder()
                .email("owner@test.com")
                .password("pass")
                .enabled(true)
                .build());
        owner.setRoles(Set.of(userRole));

        Card card = Card.builder()
                .encryptedNumber("enc")
                .maskedNumber("**** **** **** 1111")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.TEN)
                .build();
        cardRepository.save(card);

        Page<Card> cards = cardRepository.findByOwner(owner, PageRequest.of(0, 10));
        assertEquals(1, cards.getTotalElements());
        assertEquals(card.getId(), cards.getContent().get(0).getId());
    }
}