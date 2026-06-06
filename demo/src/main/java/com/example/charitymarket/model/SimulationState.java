package com.example.charitymarket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationState {

    @Id
    private Long id;

    @Column(name = "simulation_timestamp")
    private Integer currentTimestamp;
}
