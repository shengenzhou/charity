package com.example.charitymarket.service;

import com.example.charitymarket.config.AuthMode;
import com.example.charitymarket.config.HackathonAuthProperties;
import com.example.charitymarket.exception.BadRequestException;
import com.example.charitymarket.exception.NotFoundException;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String CURRENT_USER_ID = "currentUserId";

    private final UserRepository userRepository;
    private final HackathonAuthProperties authProperties;

    public User getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(CURRENT_USER_ID);

        if (userId == null) {
            if (isDemoMode()) {
                userId = 1L;
                session.setAttribute(CURRENT_USER_ID, userId);
            } else {
                throw new BadRequestException("No active player session. Use an invite link first.");
            }
        }

        Long currentUserId = userId;
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Current user not found: " + currentUserId));
    }

    public Long getCurrentUserId(HttpSession session) {
        return getCurrentUser(session).getId();
    }

    public void switchUser(Long userId, HttpSession session) {
        if (!isDemoMode()) {
            throw new BadRequestException("User switching is disabled outside demo mode.");
        }
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }
        session.setAttribute(CURRENT_USER_ID, userId);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean isDemoMode() {
        return authProperties.getMode() == AuthMode.DEMO;
    }

    public boolean isInviteMode() {
        return authProperties.getMode() == AuthMode.INVITE;
    }

    public boolean hasAuthenticatedUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(CURRENT_USER_ID);
        return userId != null && userRepository.existsById(userId);
    }

    public void loginWithInviteToken(String token, HttpSession session) {
        String normalizedToken = token == null ? "" : token.trim();
        if (normalizedToken.isBlank()) {
            throw new BadRequestException("Invite token is required.");
        }

        String email = resolveInviteEmail(normalizedToken);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Invite user not found for " + email));
        session.setAttribute(CURRENT_USER_ID, user.getId());
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    private String resolveInviteEmail(String token) {
        if (matchesToken(token, authProperties.getAliceToken())) {
            return authProperties.getAliceEmail();
        }
        if (matchesToken(token, authProperties.getBobToken())) {
            return authProperties.getBobEmail();
        }
        throw new BadRequestException("Invite token is invalid.");
    }

    private boolean matchesToken(String providedToken, String configuredToken) {
        return configuredToken != null
                && !configuredToken.isBlank()
                && configuredToken.equals(providedToken);
    }
}
