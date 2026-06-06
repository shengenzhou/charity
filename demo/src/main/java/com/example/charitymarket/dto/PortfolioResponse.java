package com.example.charitymarket.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

    private Long userId;
    private String userName;
    private BigDecimal balance;
    private String selectedCharityName;
    private BigDecimal totalRealizedPnl;
    private BigDecimal totalUnrealizedPnl;
    private BigDecimal totalPnl;
    private BigDecimal totalDonated;
    private List<PositionResponse> positions;
}
