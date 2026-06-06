package com.example.charitymarket.controller.web;

import com.example.charitymarket.model.Market;
import com.example.charitymarket.model.MarketPriceSnapshot;
import com.example.charitymarket.repository.MarketPriceSnapshotRepository;
import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.service.CurrentUserService;
import com.example.charitymarket.service.SimulationService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/simulation")
@RequiredArgsConstructor
public class WebSimulationController {

    private final SimulationService simulationService;
    private final MarketRepository marketRepository;
    private final MarketPriceSnapshotRepository marketPriceSnapshotRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String simulationPage(Model model, HttpSession session) {
        List<MarketSimulationView> marketSimulationViews = marketRepository.findAll().stream()
                .sorted(Comparator.comparing(Market::getId))
                .map(market -> new MarketSimulationView(
                        market,
                        marketPriceSnapshotRepository.findByMarketIdOrderByTimestampIndexAsc(market.getId())))
                .toList();

        model.addAttribute("currentTimestamp", simulationService.getCurrentGlobalTimestamp());
        model.addAttribute("marketSimulationViews", marketSimulationViews);
        model.addAttribute("markets", marketRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUser", currentUserService.getCurrentUser(session));
        return "simulation";
    }

    @PostMapping("/timestamp")
    public String setTimestamp(
            @RequestParam Integer timestampIndex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            simulationService.setGlobalTimestamp(timestampIndex);
            redirectAttributes.addFlashAttribute("successMessage", "Simulation moved to timestamp " + timestampIndex + ".");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:" + resolveRedirectTarget(request);
    }

    @PostMapping("/reset")
    public String resetSimulation(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        simulationService.resetSimulation();
        redirectAttributes.addFlashAttribute("successMessage", "Simulation reset.");
        return "redirect:" + resolveRedirectTarget(request);
    }

    private String resolveRedirectTarget(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/simulation";
        }

        String contextPath = request.getContextPath();
        int schemeSeparator = referer.indexOf("://");
        if (schemeSeparator >= 0) {
            int pathStart = referer.indexOf('/', schemeSeparator + 3);
            if (pathStart >= 0) {
                String path = referer.substring(pathStart);
                return path.startsWith(contextPath) ? path.substring(contextPath.length()) : path;
            }
        }
        return referer;
    }

    @Getter
    @AllArgsConstructor
    public static class MarketSimulationView {
        private final Market market;
        private final List<MarketPriceSnapshot> snapshots;
    }
}
