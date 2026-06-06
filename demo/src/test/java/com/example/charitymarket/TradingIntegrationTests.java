package com.example.charitymarket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.charitymarket.dto.TradeRequest;
import com.example.charitymarket.model.Outcome;
import com.example.charitymarket.model.TradeSide;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TradingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void buyAndSellTradesUpdatePortfolioAndDonations() throws Exception {
        TradeRequest buyRequest = TradeRequest.builder()
                .userId(1L)
                .marketId(1L)
                .outcome(Outcome.YES)
                .side(TradeSide.BUY)
                .quantity(new BigDecimal("10"))
                .build();

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeValue").value(4.20))
                .andExpect(jsonPath("$.fee").value(0.08))
                .andExpect(jsonPath("$.charityName").value("Red Cross"));

        mockMvc.perform(get("/api/users/1/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(995.72))
                .andExpect(jsonPath("$.totalDonated").value(0.08))
                .andExpect(jsonPath("$.positions[0].quantity").value(10.00))
                .andExpect(jsonPath("$.positions[0].averageEntryPrice").value(0.42))
                .andExpect(jsonPath("$.positions[0].unrealizedPnl").value(0.00));

        TradeRequest sellRequest = TradeRequest.builder()
                .userId(1L)
                .marketId(1L)
                .outcome(Outcome.YES)
                .side(TradeSide.SELL)
                .quantity(new BigDecimal("5"))
                .build();

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeValue").value(2.10))
                .andExpect(jsonPath("$.fee").value(0.04));

        mockMvc.perform(get("/api/users/1/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(997.78))
                .andExpect(jsonPath("$.totalRealizedPnl").value(0.00))
                .andExpect(jsonPath("$.totalDonated").value(0.12))
                .andExpect(jsonPath("$.positions[0].quantity").value(5.00));

        mockMvc.perform(get("/api/users/1/donations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(0.04))
                .andExpect(jsonPath("$[1].amount").value(0.08));
    }
}
