package com.example.charitymarket.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
public class WordleMatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private WordleMatch match;

    @ManyToOne(optional = false)
    private User user;

    private int attemptsUsed;
    private boolean solved;
    private boolean finished;
    private int bestProgressScore;

    private LocalDateTime joinedAt;
    private LocalDateTime finishedAt;
}
