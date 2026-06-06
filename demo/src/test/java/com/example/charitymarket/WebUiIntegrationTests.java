package com.example.charitymarket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.charitymarket.model.User;
import com.example.charitymarket.model.WordleMatch;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.repository.WordleMatchRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WebUiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WordleMatchRepository wordleMatchRepository;

    @Test
    void marketsPageRendersWithSeededContent() throws Exception {
        mockMvc.perform(get("/markets"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Markets")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Will it rain in Amsterdam tomorrow?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alice")));
    }

    @Test
    void webTradeFlowRedirectsToPortfolioAndUpdatesSessionUser() throws Exception {
        MvcResult switchResult = mockMvc.perform(post("/users/switch").param("userId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/markets"))
                .andReturn();

        mockMvc.perform(post("/trades")
                        .session((org.springframework.mock.web.MockHttpSession) switchResult.getRequest().getSession(false))
                        .param("userId", "2")
                        .param("marketId", "1")
                        .param("outcome", "YES")
                        .param("side", "BUY")
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portfolio"));

        mockMvc.perform(get("/portfolio")
                        .session((org.springframework.mock.web.MockHttpSession) switchResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Portfolio")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bob")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("WWF")));
    }

    @Test
    void gamesPageRendersWithWordleLobby() throws Exception {
        mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Games")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Create duel")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("All games")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1% each side")));
    }

    @Test
    void wordleMatchAwardsPrizeToFastestPlayerOnAttemptTie() throws Exception {
        BigDecimal aliceStartingBalance = userRepository.findById(1L).orElseThrow().getBalance();
        BigDecimal bobStartingBalance = userRepository.findById(2L).orElseThrow().getBalance();

        MvcResult aliceSessionResult = mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession aliceSession = (MockHttpSession) aliceSessionResult.getRequest().getSession(false);

        mockMvc.perform(post("/games")
                        .session(aliceSession)
                        .param("gameType", "WORDLE")
                        .param("betAmount", "25.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games"));

        WordleMatch createdMatch = wordleMatchRepository.findAll().stream()
                .findFirst()
                .orElseThrow();
        String solutionWord = createdMatch.getSolutionWord();
        String wrongGuess = "BRAVE".equals(solutionWord) ? "QUEST" : "BRAVE";

        MvcResult bobSwitchResult = mockMvc.perform(post("/users/switch")
                        .param("userId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/markets"))
                .andReturn();
        MockHttpSession bobSession = (MockHttpSession) bobSwitchResult.getRequest().getSession(false);

        mockMvc.perform(post("/games/join")
                        .session(bobSession)
                        .param("matchId", createdMatch.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games/" + createdMatch.getId()));

        mockMvc.perform(post("/games/" + createdMatch.getId() + "/guess")
                        .session(aliceSession)
                        .param("guess", wrongGuess))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games/" + createdMatch.getId()));

        mockMvc.perform(post("/games/" + createdMatch.getId() + "/guess")
                        .session(aliceSession)
                        .param("guess", solutionWord))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games/" + createdMatch.getId()));

        Thread.sleep(25L);

        mockMvc.perform(post("/games/" + createdMatch.getId() + "/guess")
                        .session(bobSession)
                        .param("guess", wrongGuess))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games/" + createdMatch.getId()));

        Thread.sleep(25L);

        mockMvc.perform(post("/games/" + createdMatch.getId() + "/guess")
                        .session(bobSession)
                        .param("guess", solutionWord))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games/" + createdMatch.getId()));

        WordleMatch settledMatch = wordleMatchRepository.findById(createdMatch.getId()).orElseThrow();
        User alice = userRepository.findById(1L).orElseThrow();
        User bob = userRepository.findById(2L).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(settledMatch.getWinner().getName()).isEqualTo("Alice");
        org.assertj.core.api.Assertions.assertThat(alice.getBalance())
                .isEqualByComparingTo(aliceStartingBalance.subtract(new BigDecimal("25.25")).add(new BigDecimal("50.00")));
        org.assertj.core.api.Assertions.assertThat(bob.getBalance())
                .isEqualByComparingTo(bobStartingBalance.subtract(new BigDecimal("25.25")));

        mockMvc.perform(get("/games/" + createdMatch.getId()).session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alice won on the time tiebreak.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Solution:")));

        mockMvc.perform(get("/games").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Alice won on the time tiebreak."))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No live matches for this filter")));
    }
}
