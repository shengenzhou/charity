package com.example.charitymarket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.charitymarket.dto.TradeRequest;
import com.example.charitymarket.model.Market;
import com.example.charitymarket.model.MarketPriceSnapshot;
import com.example.charitymarket.model.MarketStatus;
import com.example.charitymarket.model.Outcome;
import com.example.charitymarket.model.Position;
import com.example.charitymarket.model.TradeSide;
import com.example.charitymarket.repository.MarketPriceSnapshotRepository;
import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.PositionRepository;
import com.example.charitymarket.repository.SimulationStateRepository;
import com.example.charitymarket.repository.TradeRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.service.SimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private MarketPriceSnapshotRepository marketPriceSnapshotRepository;

    @Autowired
    private SimulationStateRepository simulationStateRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @BeforeEach
    void resetSimulation() {
        simulationService.resetSimulation();
    }

    @Test
    void simulationMovesForwardAndRejectsBackwardMovement() throws Exception {
        mockMvc.perform(get("/simulation"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Simulation")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Stored market snapshots")));

        Market initialMarket = marketRepository.findById(1L).orElseThrow();
        MarketPriceSnapshot timestampOneSnapshot = marketPriceSnapshotRepository
                .findByMarketIdAndTimestampIndex(1L, 1)
                .orElseThrow();

        mockMvc.perform(post("/simulation/timestamp").param("timestampIndex", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation"));

        Market movedMarket = marketRepository.findById(1L).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(movedMarket.getYesPrice()).isEqualByComparingTo(timestampOneSnapshot.getYesPrice());
        org.assertj.core.api.Assertions.assertThat(movedMarket.getNoPrice()).isEqualByComparingTo(timestampOneSnapshot.getNoPrice());
        org.assertj.core.api.Assertions.assertThat(movedMarket.getYesPrice()).isNotEqualByComparingTo(initialMarket.getYesPrice());
        org.assertj.core.api.Assertions.assertThat(simulationStateRepository.findById(1L).orElseThrow().getCurrentTimestamp()).isEqualTo(1);

        mockMvc.perform(post("/simulation/timestamp").param("timestampIndex", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation"));

        org.assertj.core.api.Assertions.assertThat(simulationStateRepository.findById(1L).orElseThrow().getCurrentTimestamp()).isEqualTo(1);
    }

    @Test
    void timestampFourResolvesMarketsPaysOutPositionsAndResetRestoresDemoState() throws Exception {
        TradeRequest aliceTrade = TradeRequest.builder()
                .userId(1L)
                .marketId(1L)
                .outcome(Outcome.YES)
                .side(TradeSide.BUY)
                .quantity(new BigDecimal("10"))
                .build();
        TradeRequest bobTrade = TradeRequest.builder()
                .userId(2L)
                .marketId(1L)
                .outcome(Outcome.NO)
                .side(TradeSide.BUY)
                .quantity(new BigDecimal("10"))
                .build();

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aliceTrade)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bobTrade)))
                .andExpect(status().isOk());

        MarketPriceSnapshot finalSnapshot = marketPriceSnapshotRepository
                .findByMarketIdAndTimestampIndex(1L, 4)
                .orElseThrow();
        Outcome resolvedOutcome = finalSnapshot.getYesPrice().compareTo(new BigDecimal("1.00")) == 0 ? Outcome.YES : Outcome.NO;

        mockMvc.perform(post("/simulation/timestamp").param("timestampIndex", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation"));

        Market resolvedMarket = marketRepository.findById(1L).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(resolvedMarket.getStatus()).isEqualTo(MarketStatus.RESOLVED);
        org.assertj.core.api.Assertions.assertThat(resolvedMarket.isPayoutCompleted()).isTrue();
        org.assertj.core.api.Assertions.assertThat(resolvedMarket.getResolvedOutcome()).isEqualTo(resolvedOutcome);

        Position alicePosition = positionRepository.findByUserIdAndMarketIdAndOutcome(1L, 1L, Outcome.YES).orElseThrow();
        Position bobPosition = positionRepository.findByUserIdAndMarketIdAndOutcome(2L, 1L, Outcome.NO).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(alicePosition.getQuantity()).isEqualByComparingTo("0.00");
        org.assertj.core.api.Assertions.assertThat(bobPosition.getQuantity()).isEqualByComparingTo("0.00");

        BigDecimal aliceExpectedBalance = resolvedOutcome == Outcome.YES
                ? new BigDecimal("1005.72")
                : new BigDecimal("995.72");
        BigDecimal bobExpectedBalance = resolvedOutcome == Outcome.NO
                ? new BigDecimal("1004.08")
                : new BigDecimal("994.08");
        org.assertj.core.api.Assertions.assertThat(userRepository.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo(aliceExpectedBalance);
        org.assertj.core.api.Assertions.assertThat(userRepository.findById(2L).orElseThrow().getBalance())
                .isEqualByComparingTo(bobExpectedBalance);

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aliceTrade)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/simulation/reset"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation"));

        org.assertj.core.api.Assertions.assertThat(simulationStateRepository.findById(1L).orElseThrow().getCurrentTimestamp()).isEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(tradeRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(positionRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(userRepository.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo("1000.00");
        org.assertj.core.api.Assertions.assertThat(marketRepository.findById(1L).orElseThrow().getStatus())
                .isEqualTo(MarketStatus.OPEN);
        org.assertj.core.api.Assertions.assertThat(marketPriceSnapshotRepository.findByMarketIdOrderByTimestampIndexAsc(1L)).hasSize(5);
    }
}
