package com.example.charitymarket.repository;

import com.example.charitymarket.model.Market;
import com.example.charitymarket.model.MarketPriceSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketPriceSnapshotRepository extends JpaRepository<MarketPriceSnapshot, Long> {

    List<MarketPriceSnapshot> findByMarketOrderByTimestampIndex(Market market);

    Optional<MarketPriceSnapshot> findByMarketIdAndTimestampIndex(Long marketId, Integer timestampIndex);

    Optional<MarketPriceSnapshot> findByMarketAndTimestampIndex(Market market, Integer timestampIndex);

    List<MarketPriceSnapshot> findByMarketIdOrderByTimestampIndexAsc(Long marketId);

    List<MarketPriceSnapshot> findByTimestampIndex(Integer timestampIndex);

    void deleteByMarketId(Long marketId);

    void deleteAllByMarket(Market market);
}
