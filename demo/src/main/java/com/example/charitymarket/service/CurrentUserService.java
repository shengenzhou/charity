package com.example.charitymarket.service;

import com.example.charitymarket.exception.NotFoundException;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String CURRENT_USER_ID = "currentUserId";

    private final UserRepository userRepository;

    public User getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(CURRENT_USER_ID);

        if (userId == null) {
            userId = 1L;
            session.setAttribute(CURRENT_USER_ID, userId);
        }

        Long currentUserId = userId;
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Current user not found: " + currentUserId));
    }

    public Long getCurrentUserId(HttpSession session) {
        return getCurrentUser(session).getId();
    }

    public void switchUser(Long userId, HttpSession session) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }
        session.setAttribute(CURRENT_USER_ID, userId);
    }
}
