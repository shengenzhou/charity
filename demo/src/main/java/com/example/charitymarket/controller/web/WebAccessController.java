package com.example.charitymarket.controller.web;

import com.example.charitymarket.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebAccessController {

    private final CurrentUserService currentUserService;

    @GetMapping("/access")
    public String access(HttpSession session) {
        if (!currentUserService.isInviteMode()) {
            return "redirect:/markets";
        }

        if (currentUserService.hasAuthenticatedUser(session)) {
            return "redirect:/markets";
        }

        return "access";
    }

    @PostMapping("/access")
    public String submitUsername(
            @RequestParam String username,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!currentUserService.isInviteMode()) {
            return "redirect:/markets";
        }
        if (currentUserService.hasAuthenticatedUser(session)) {
            return "redirect:/markets";
        }

        try {
            currentUserService.loginOrRegisterWithUsername(session, username);
            redirectAttributes.addFlashAttribute("successMessage", "You are in. Good luck.");
            return "redirect:/markets";
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/access";
        }
    }

    @GetMapping("/access/username")
    public String usernameSetup() {
        return "redirect:/access";
    }

    @PostMapping("/access/username")
    public String submitLegacyUsername(
            @RequestParam String username,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        return submitUsername(username, session, redirectAttributes);
    }

    @PostMapping("/access/logout")
    public String logout(HttpSession session) {
        currentUserService.logout(session);
        return "redirect:/access";
    }
}
