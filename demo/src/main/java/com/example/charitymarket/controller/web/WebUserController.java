package com.example.charitymarket.controller.web;

import com.example.charitymarket.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class WebUserController {

    private final CurrentUserService currentUserService;

    @PostMapping("/users/switch")
    public String switchUser(
            @RequestParam Long userId,
            HttpSession session,
            HttpServletRequest request) {
        currentUserService.switchUser(userId, session);

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }

        return "redirect:/markets";
    }
}
