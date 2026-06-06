package com.example.charitymarket.controller.web;

import com.example.charitymarket.repository.TradeRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.service.CurrentUserService;
import com.example.charitymarket.service.PortfolioService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class WebPortfolioController {

    private final PortfolioService portfolioService;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @GetMapping("/portfolio")
    public String portfolio(Model model, HttpSession session) {
        Long userId = currentUserService.getCurrentUserId(session);
        model.addAttribute("portfolio", portfolioService.getPortfolio(userId));
        model.addAttribute("currentUser", currentUserService.getCurrentUser(session));
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("trades", tradeRepository.findByUserIdOrderByCreatedAtDesc(userId));
        model.addAttribute("settledPositions", portfolioService.getAllPositions(userId).stream()
                .filter(position -> position.getQuantity().compareTo(BigDecimal.ZERO) == 0)
                .toList());
        return "portfolio";
    }
}
