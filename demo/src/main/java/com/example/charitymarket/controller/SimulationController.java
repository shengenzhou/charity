package com.example.charitymarket.controller;

import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.service.SimulationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;
    private final MarketRepository marketRepository;

    @GetMapping
    public Map<String, Object> getState() {
        return Map.of(
                "currentTimestamp", simulationService.getCurrentGlobalTimestamp(),
                "markets", marketRepository.findAll());
    }

    @PostMapping("/timestamp")
    public Map<String, Object> setTimestamp(@RequestParam Integer timestampIndex) {
        simulationService.setGlobalTimestamp(timestampIndex);
        return Map.of("currentTimestamp", simulationService.getCurrentGlobalTimestamp());
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        simulationService.resetSimulation();
        return Map.of("currentTimestamp", simulationService.getCurrentGlobalTimestamp());
    }
}
