package com.example.charitymarket.controller.web;

import com.example.charitymarket.model.Charity;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.CharityRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
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

    private final CharityRepository charityRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @GetMapping("/charities")
    public String charities(Model model, HttpSession session) {
        model.addAttribute("charities", charityRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUser", currentUserService.getCurrentUser(session));
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
