package com.example.charitymarket.controller.web;

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
public class WebAccessController {

    private final CurrentUserService currentUserService;

    @GetMapping("/access")
    public String access(
            @RequestParam(required = false) String token,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!currentUserService.isInviteMode()) {
            return "redirect:/markets";
        }

        if (currentUserService.hasAuthenticatedUser(session)) {
            return "redirect:/markets";
        }

        if (token != null && !token.isBlank()) {
            try {
                currentUserService.loginWithInviteToken(token, session);
                redirectAttributes.addFlashAttribute("successMessage", "Access granted. You're signed in.");
                return "redirect:/markets";
            } catch (RuntimeException exception) {
                model.addAttribute("errorMessage", exception.getMessage());
            }
        }

        return "access";
    }

    @PostMapping("/access")
    public String submitAccessToken(
            @RequestParam String token,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!currentUserService.isInviteMode()) {
            return "redirect:/markets";
        }

        try {
            currentUserService.loginWithInviteToken(token, session);
            redirectAttributes.addFlashAttribute("successMessage", "Access granted. You're signed in.");
            return "redirect:/markets";
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/access";
        }
    }

    @PostMapping("/access/logout")
    public String logout(HttpSession session) {
        currentUserService.logout(session);
        return "redirect:/access";
    }
}
