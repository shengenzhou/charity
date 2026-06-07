package com.example.charitymarket.controller.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.charitymarket.model.Market;
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

    private boolean isSportMarket(Market m) {
        return m.getQuestion().startsWith("Sport -");
    }
@GetMapping("/markets")
    public String markets(
            @RequestParam(defaultValue = "grid") String view,
            Model model,
            HttpSession session) {

        List<Market> allMarkets = marketRepository.findAll();
        List<Market> filteredMarkets;

        // --- NEW FILTERING LOGIC ---
        if ("sport".equals(view)) {
            // Show ONLY sport markets
            filteredMarkets = allMarkets.stream()
                .filter(this::isSportMarket)
                .toList();
        } else if ("grid".equals(view)) {
            // Show ONLY charity markets (exclude sport)
            filteredMarkets = allMarkets.stream()
                .filter(m -> !isSportMarket(m))
                .toList();
        } else {
            // Default/Graph view: show everything
            filteredMarkets = allMarkets;
        }

        model.addAttribute("view", view);
        model.addAttribute("markets", filteredMarkets); 
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUser", currentUserService.getCurrentUser(session));
        
        Integer currentTimestamp = simulationService.getCurrentGlobalTimestamp();
        model.addAttribute("currentTimestamp", currentTimestamp); 

        if ("graph".equals(view)) {
            model.addAttribute("marketSimulationViews", filteredMarkets.stream()
                .map(market -> new WebSimulationController.MarketSimulationView(
                    market,
                    marketPriceSnapshotRepository.findByMarketIdOrderByTimestampIndexAsc(market.getId())
                )).toList());
        }
        return "markets";
    }

}
