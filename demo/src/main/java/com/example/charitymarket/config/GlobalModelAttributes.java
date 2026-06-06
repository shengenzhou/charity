package com.example.charitymarket.config;

import com.example.charitymarket.dto.SimulationState;
import com.example.charitymarket.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SimulationService simulationService;

    @ModelAttribute("simulationState")
    public SimulationState simulationState() {
        return simulationService.getSimulationState();
    }
}
