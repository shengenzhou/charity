package com.example.charitymarket.config;

import com.example.charitymarket.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class WebAuthInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUserService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!currentUserService.isInviteMode()) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null && currentUserService.hasAuthenticatedUser(session)) {
            return true;
        }

        response.sendRedirect("/access");
        return false;
    }
}
