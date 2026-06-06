package com.example.charitymarket.controller.web;

import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.service.CurrentUserService;
import com.example.charitymarket.service.SimulationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class WebMarketController {

    private final MarketRepository marketRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final SimulationService simulationService;

    @GetMapping("/")
    public String home() {
        return "redirect:/markets";
    }

    @GetMapping("/markets")
    public String markets(Model model, HttpSession session) {
        model.addAttribute("markets", marketRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUser", currentUserService.getCurrentUser(session));
        model.addAttribute("currentTimestamp", simulationService.getCurrentGlobalTimestamp());
        return "markets";
    }
}
