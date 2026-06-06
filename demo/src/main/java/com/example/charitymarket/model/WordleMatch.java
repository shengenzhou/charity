package com.example.charitymarket.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordleMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User creator;

    @ManyToOne
    private User opponent;

    @ManyToOne
    private User winner;

    private GameType gameType;
    private String solutionWord;
    private BigDecimal betAmount;
    private BigDecimal feeAmountPerPlayer;
    private BigDecimal prizePool;
    private String resultSummary;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private WordleMatchStatus status;
}
