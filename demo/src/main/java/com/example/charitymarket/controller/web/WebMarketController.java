package com.example.charitymarket.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.charitymarket.repository.MarketPriceSnapshotRepository;
import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.service.CurrentUserService;
import com.example.charitymarket.service.SimulationService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebMarketController {

    private final MarketRepository marketRepository;
    private final MarketPriceSnapshotRepository marketPriceSnapshotRepository; 
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final SimulationService simulationService;

    @GetMapping("/")
    public String home() {
        return "redirect:/markets";
    }
    
    @GetMapping("/markets")
    public String markets(
            @RequestParam(defaultValue = "grid") String view, // Capture grid vs graph view
            Model model,
            HttpSession session) {

        model.addAttribute("view", view);
        model.addAttribute("markets", marketRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUser", currentUserService.getCurrentUser(session));
        
        // Read the active timestamp from the simulation panel
        Integer currentTimestamp = simulationService.getCurrentGlobalTimestamp();
        model.addAttribute("currentTimestamp", currentTimestamp); 

        // Fetch snapshot history only when the user is in graph view
        if ("graph".equals(view)) {
            model.addAttribute("marketSimulationViews", marketRepository.findAll().stream()
                .map(market -> new WebSimulationController.MarketSimulationView(
                    market,
                    marketPriceSnapshotRepository.findByMarketIdOrderByTimestampIndexAsc(market.getId())
                )).toList());
        }
        return "markets";
    }
}
