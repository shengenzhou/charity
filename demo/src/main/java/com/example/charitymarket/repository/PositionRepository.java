package com.example.charitymarket.repository;

import com.example.charitymarket.model.Outcome;
import com.example.charitymarket.model.Position;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByUserIdAndMarketIdAndOutcome(Long userId, Long marketId, Outcome outcome);

    List<Position> findByUserId(Long userId);

    List<Position> findByMarketId(Long marketId);
}
