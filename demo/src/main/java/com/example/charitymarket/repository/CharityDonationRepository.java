package com.example.charitymarket.repository;

import com.example.charitymarket.model.CharityDonation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CharityDonationRepository extends JpaRepository<CharityDonation, Long> {

    List<CharityDonation> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<CharityDonation> findAllByWordleMatchId(Long wordleMatchId);

    @Query("""
            select coalesce(sum(d.amount), 0)
            from CharityDonation d
            where d.createdAt >= :startOfDay and d.createdAt < :endOfDay
            """)
    BigDecimal sumAmountCreatedBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
