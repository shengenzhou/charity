package com.example.charitymarket.controller;

import com.example.charitymarket.dto.DonationResponse;
import com.example.charitymarket.dto.PortfolioResponse;
import com.example.charitymarket.dto.PositionResponse;
import com.example.charitymarket.service.PortfolioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/positions")
    public List<PositionResponse> getPositions(@PathVariable Long userId) {
        return portfolioService.getPositions(userId);
    }

    @GetMapping("/portfolio")
    public PortfolioResponse getPortfolio(@PathVariable Long userId) {
        return portfolioService.getPortfolio(userId);
    }

    @GetMapping("/donations")
    public List<DonationResponse> getDonations(@PathVariable Long userId) {
        return portfolioService.getDonations(userId);
    }
}
