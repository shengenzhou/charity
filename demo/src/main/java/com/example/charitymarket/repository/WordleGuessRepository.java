package com.example.charitymarket.repository;

import com.example.charitymarket.model.WordleGuess;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordleGuessRepository extends JpaRepository<WordleGuess, Long> {

    List<WordleGuess> findAllByPlayerIdOrderByGuessNumberAsc(Long playerId);

    void deleteAllByMatchId(Long matchId);
}
