package com.example.charitymarket.repository;

import com.example.charitymarket.model.Market;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Market, Long> {
}
