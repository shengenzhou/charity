package com.example.charitymarket.dto.wordle;

import com.example.charitymarket.model.GameType;
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
public class CreateMatchRequest {

    @NotNull(message = "Game is required")
    private GameType gameType;

    @NotNull(message = "Bet amount is required")
    @DecimalMin(value = "1.00", message = "Bet amount must be at least 1.00")
    private BigDecimal betAmount;
}
