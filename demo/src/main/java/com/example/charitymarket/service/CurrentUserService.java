package com.example.charitymarket.service;

import com.example.charitymarket.config.AuthMode;
import com.example.charitymarket.config.HackathonAuthProperties;
import com.example.charitymarket.exception.BadRequestException;
import com.example.charitymarket.exception.NotFoundException;
import com.example.charitymarket.model.Charity;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.CharityRepository;
import com.example.charitymarket.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String CURRENT_USER_ID = "currentUserId";
    private static final int MAX_PLAYERS = 100;
    private static final BigDecimal STARTING_BALANCE = new BigDecimal("1000.00");

    private final UserRepository userRepository;
    private final CharityRepository charityRepository;
    private final HackathonAuthProperties authProperties;

    public User getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(CURRENT_USER_ID);

        if (userId == null) {
            if (isDemoMode()) {
                userId = 1L;
                session.setAttribute(CURRENT_USER_ID, userId);
            } else {
                throw new BadRequestException("No active player session. Pick a username first.");
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
        throw new BadRequestException("Account switching is disabled.");
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

    public void loginOrRegisterWithUsername(HttpSession session, String username) {
        String normalizedUsername = normalizeUsername(username);

        Optional<User> existingUser = userRepository.findAll().stream()
                .filter(User::isUsernameConfigured)
                .filter(user -> user.getName() != null)
                .filter(user -> user.getName().equalsIgnoreCase(normalizedUsername))
                .findFirst();
        if (existingUser.isPresent()) {
            session.setAttribute(CURRENT_USER_ID, existingUser.get().getId());
            return;
        }

        List<User> allUsers = userRepository.findAll();
        User availableUser = allUsers.stream()
                .filter(user -> !user.isUsernameConfigured())
                .findFirst()
                .orElseGet(() -> createNewInviteUser(allUsers.size()));

        availableUser.setName(normalizedUsername);
        availableUser.setUsernameConfigured(true);
        userRepository.save(availableUser);
        session.setAttribute(CURRENT_USER_ID, availableUser.getId());
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    private String normalizeUsername(String username) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.length() < 2 || normalizedUsername.length() > 20) {
            throw new BadRequestException("Username must be between 2 and 20 characters.");
        }
        if (!normalizedUsername.matches("[A-Za-z0-9 _-]+")) {
            throw new BadRequestException("Username can only contain letters, numbers, spaces, - and _.");
        }
        return normalizedUsername;
    }

    private User createNewInviteUser(int existingUserCount) {
        if (existingUserCount >= MAX_PLAYERS) {
            throw new BadRequestException("The player limit has been reached.");
        }

        Charity defaultCharity = charityRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No charities are available yet."));

        return User.builder()
                .balance(STARTING_BALANCE)
                .selectedCharity(defaultCharity)
                .usernameConfigured(false)
                .build();
    }
}
