package com.example.charitymarket.controller.web;

import com.example.charitymarket.model.Charity;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.CharityDonationRepository;
import com.example.charitymarket.repository.CharityRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebCharityController {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final CharityDonationRepository charityDonationRepository;
    private final CharityRepository charityRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @GetMapping("/charities")
    public String charities(Model model, HttpSession session) {
        List<Charity> charities = charityRepository.findAll();
        Map<Long, BigDecimal> charityTotals = new LinkedHashMap<>();
        for (Charity charity : charities) {
            charityTotals.put(charity.getId(), charityDonationRepository.sumAmountByCharityId(charity.getId()));
        }

        User currentUser = currentUserService.getCurrentUser(session);
        BigDecimal selectedCharityTotal = currentUser.getSelectedCharity() == null
                ? ZERO
                : charityTotals.getOrDefault(currentUser.getSelectedCharity().getId(), ZERO);

        model.addAttribute("charities", charities);
        model.addAttribute("charityTotals", charityTotals);
        model.addAttribute("selectedCharityTotal", selectedCharityTotal);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUser", currentUser);
        return "charities";
    }

    @PostMapping("/charities/select")
    public String selectCharity(
            @RequestParam Long charityId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User currentUser = currentUserService.getCurrentUser(session);
        Charity charity = charityRepository.findById(charityId)
                .orElseThrow(() -> new IllegalArgumentException("Charity not found: " + charityId));

        currentUser.setSelectedCharity(charity);
        userRepository.save(currentUser);

        redirectAttributes.addFlashAttribute("successMessage", "Selected charity updated.");
        return "redirect:/charities";
    }
}
