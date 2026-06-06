package com.example.charitymarket.dto;

import com.example.charitymarket.model.Outcome;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionResponse {

    private Long id;
    private Long marketId;
    private String marketQuestion;
    private Outcome outcome;
    private BigDecimal quantity;
    private BigDecimal averageEntryPrice;
    private BigDecimal currentPrice;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    private BigDecimal totalPnl;
}
