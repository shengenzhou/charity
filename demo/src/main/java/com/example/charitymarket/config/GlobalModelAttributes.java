package com.example.charitymarket.config;

import com.example.charitymarket.dto.SimulationState;
import com.example.charitymarket.repository.CharityDonationRepository;
import com.example.charitymarket.service.CurrentUserService;
import com.example.charitymarket.service.SimulationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SimulationService simulationService;
    private final CurrentUserService currentUserService;
    private final CharityDonationRepository charityDonationRepository;

    @ModelAttribute("simulationState")
    public SimulationState simulationState() {
        return simulationService.getSimulationState();
    }

    @ModelAttribute("demoMode")
    public boolean demoMode() {
        return currentUserService.isDemoMode();
    }

    @ModelAttribute("todayCharityDonations")
    public BigDecimal todayCharityDonations() {
        LocalDate today = LocalDate.now();
        return charityDonationRepository.sumAmountCreatedBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }
}
