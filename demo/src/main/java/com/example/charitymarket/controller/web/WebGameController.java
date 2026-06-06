package com.example.charitymarket.controller.web;

import com.example.charitymarket.dto.wordle.CreateMatchRequest;
import com.example.charitymarket.dto.wordle.GuessRequest;
import com.example.charitymarket.dto.wordle.JoinMatchRequest;
import com.example.charitymarket.model.GameType;
import com.example.charitymarket.service.CurrentUserService;
import com.example.charitymarket.service.WordleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebGameController {

    private final WordleService wordleService;
    private final CurrentUserService currentUserService;

    @GetMapping("/games")
    public String games(
            @RequestParam(required = false) GameType gameType,
            @RequestParam(required = false) BigDecimal minBetAmount,
            @RequestParam(required = false) BigDecimal maxBetAmount,
            Model model,
            HttpSession session) {
        Long currentUserId = currentUserService.getCurrentUserId(session);
        model.addAttribute("matches", wordleService.getMatchesForLobby(currentUserId, gameType, minBetAmount, maxBetAmount));
        model.addAttribute("users", currentUserService.getAllUsers());
        model.addAttribute("currentUser", currentUserService.getCurrentUser(session));
        model.addAttribute("gameOptions", gameOptions());
        model.addAttribute("selectedGameType", gameType);
        model.addAttribute("minBetAmount", minBetAmount);
        model.addAttribute("maxBetAmount", maxBetAmount);
        if (!model.containsAttribute("createMatchRequest")) {
            model.addAttribute("createMatchRequest", CreateMatchRequest.builder().gameType(GameType.WORDLE).build());
        }
        return "games";
    }

    @GetMapping("/games/{matchId}")
    public String matchDetail(@PathVariable Long matchId, Model model, HttpSession session) {
        Long currentUserId = currentUserService.getCurrentUserId(session);
        model.addAttribute("match", wordleService.getMatch(matchId, currentUserId));
        model.addAttribute("users", currentUserService.getAllUsers());
        model.addAttribute("currentUser", currentUserService.getCurrentUser(session));
        if (!model.containsAttribute("guessRequest")) {
            model.addAttribute("guessRequest", GuessRequest.builder().build());
        }
        return "game-match";
    }

    @PostMapping("/games")
    public String createMatch(
            @Valid @ModelAttribute CreateMatchRequest createMatchRequest,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.createMatchRequest", bindingResult);
            redirectAttributes.addFlashAttribute("createMatchRequest", createMatchRequest);
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "Invalid match request.");
            return "redirect:/games";
        }

        try {
            wordleService.createMatch(currentUserService.getCurrentUserId(session), createMatchRequest);
            redirectAttributes.addFlashAttribute("successMessage", createMatchRequest.getGameType() + " duel created. Switch accounts to join it.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/games";
    }

    @PostMapping("/games/join")
    public String joinMatch(
            @Valid @ModelAttribute JoinMatchRequest joinMatchRequest,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to join that match.");
            return "redirect:/games";
        }

        try {
            wordleService.joinMatch(currentUserService.getCurrentUserId(session), joinMatchRequest.getMatchId());
            redirectAttributes.addFlashAttribute("successMessage", "Match joined. The duel is live.");
            return "redirect:/games/" + joinMatchRequest.getMatchId();
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/games";
        }
    }

    @PostMapping("/games/{matchId}/guess")
    public String submitGuess(
            @PathVariable Long matchId,
            @Valid @ModelAttribute GuessRequest guessRequest,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.guessRequest", bindingResult);
            redirectAttributes.addFlashAttribute("guessRequest", guessRequest);
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "Invalid guess.");
            return "redirect:/games/" + matchId;
        }

        try {
            wordleService.submitGuess(currentUserService.getCurrentUserId(session), matchId, guessRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Guess submitted.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/games/" + matchId;
    }

    private List<GameOptionView> gameOptions() {
        return List.of(
                new GameOptionView(GameType.WORDLE, "Wordle", "Live"),
                new GameOptionView(GameType.TRIVIA, "Trivia", "Coming soon"),
                new GameOptionView(GameType.MEMORY_DUEL, "Memory Duel", "Coming soon"));
    }

    public record GameOptionView(GameType value, String label, String availability) {
    }
}
