package com.example.charitymarket.controller.web;

import com.example.charitymarket.dto.TradeRequest;
import com.example.charitymarket.model.Market;
import com.example.charitymarket.model.Outcome;
import com.example.charitymarket.model.TradeSide;
import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.service.CurrentUserService;
import com.example.charitymarket.service.TradeService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebTradeController {

    private final TradeService tradeService;
    private final MarketRepository marketRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @GetMapping("/trades/new")
    public String newTradeForm(
            @RequestParam Long marketId,
            @RequestParam Outcome outcome,
            @RequestParam TradeSide side,
            Model model,
            HttpSession session) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new IllegalArgumentException("Market not found: " + marketId));

        TradeRequest tradeRequest = TradeRequest.builder()
                .userId(currentUserService.getCurrentUserId(session))
                .marketId(marketId)
                .outcome(outcome)
                .side(side)
                .build();

        populateTradeFormModel(model, session, market, tradeRequest, null);
        return "trade-form";
    }

    @PostMapping("/trades")
    public String submitTrade(
            @Valid @ModelAttribute TradeRequest tradeRequest,
            BindingResult bindingResult,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        tradeRequest.setUserId(currentUserService.getCurrentUserId(session));

        Market market = marketRepository.findById(tradeRequest.getMarketId())
                .orElseThrow(() -> new IllegalArgumentException("Market not found: " + tradeRequest.getMarketId()));

        if (bindingResult.hasErrors()) {
            populateTradeFormModel(model, session, market, tradeRequest, null);
            return "trade-form";
        }

        try {
            tradeService.executeTrade(tradeRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Trade executed successfully.");
            return "redirect:/portfolio";
        } catch (RuntimeException exception) {
            populateTradeFormModel(model, session, market, tradeRequest, exception.getMessage());
            return "trade-form";
        }
    }

    private void populateTradeFormModel(
            Model model,
            HttpSession session,
            Market market,
            TradeRequest tradeRequest,
            String errorMessage) {
        model.addAttribute("tradeRequest", tradeRequest);
        model.addAttribute("market", market);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUser", currentUserService.getCurrentUser(session));
        model.addAttribute("selectedOutcome", tradeRequest.getOutcome());
        model.addAttribute("selectedSide", tradeRequest.getSide());
        model.addAttribute(
                "currentPrice",
                tradeRequest.getOutcome() == Outcome.YES ? market.getYesPrice() : market.getNoPrice());
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }
    }
}
