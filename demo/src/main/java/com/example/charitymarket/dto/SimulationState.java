package com.example.charitymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationState {

    private Integer currentTimestamp;
    private Integer expiryTimestamp;
    private boolean resolved;
}
