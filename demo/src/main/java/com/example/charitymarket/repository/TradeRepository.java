package com.example.charitymarket.repository;

import com.example.charitymarket.model.Trade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUserIdOrderByCreatedAtDesc(Long userId);
}
