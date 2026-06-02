package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Page<Card> findByOwner(User owner, Pageable pageable);

    Optional<Card> findByIdAndOwner(Long id, User owner);

    @Query("SELECT c FROM Card c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:ownerId IS NULL OR c.owner.id = :ownerId)")
    Page<Card> findAllWithFilters(@Param("status") CardStatus status,
                                  @Param("ownerId") Long ownerId,
                                  Pageable pageable);

    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);
}