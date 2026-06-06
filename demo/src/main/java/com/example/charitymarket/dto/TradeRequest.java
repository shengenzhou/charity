package com.example.charitymarket.dto;

import com.example.charitymarket.model.Outcome;
import com.example.charitymarket.model.TradeSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long marketId;

    @NotNull
    private Outcome outcome;

    @NotNull
    private TradeSide side;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantity;
}
