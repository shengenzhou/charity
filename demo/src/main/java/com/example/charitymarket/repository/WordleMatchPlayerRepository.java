package com.example.charitymarket.repository;

import com.example.charitymarket.model.WordleMatchPlayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordleMatchPlayerRepository extends JpaRepository<WordleMatchPlayer, Long> {

    List<WordleMatchPlayer> findAllByMatchIdOrderByIdAsc(Long matchId);

    Optional<WordleMatchPlayer> findByMatchIdAndUserId(Long matchId, Long userId);
}
