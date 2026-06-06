package com.example.charitymarket.dto;

import com.example.charitymarket.model.Outcome;
import com.example.charitymarket.model.TradeSide;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long marketId;
    private String marketQuestion;
    private Outcome outcome;
    private TradeSide side;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal tradeValue;
    private BigDecimal fee;
    private String charityName;
    private LocalDateTime createdAt;
}
