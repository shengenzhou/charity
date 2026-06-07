package com.example.charitymarket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.auth.mode=DEMO")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WebUiIntegrationTests {

    private static final String CURRENT_USER_ID = "currentUserId";

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
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Will the malaria incidence rate in Region X decrease by at least 15% by December 2028?")));
    }

    @Test
    void webTradeFlowRedirectsToPortfolioAndUpdatesSessionUser() throws Exception {
        MockHttpSession bobSession = new MockHttpSession();
        bobSession.setAttribute(CURRENT_USER_ID, 2L);

        mockMvc.perform(post("/trades")
                        .session(bobSession)
                        .param("userId", "2")
                        .param("marketId", "1")
                        .param("outcome", "YES")
                        .param("side", "BUY")
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portfolio"));

        mockMvc.perform(get("/portfolio")
                        .session(bobSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Portfolio")))
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

        MockHttpSession bobSession = new MockHttpSession();
        bobSession.setAttribute(CURRENT_USER_ID, 2L);

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

        org.assertj.core.api.Assertions.assertThat(settledMatch.getWinner().getId()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(alice.getBalance())
                .isEqualByComparingTo(aliceStartingBalance.subtract(new BigDecimal("25.25")).add(new BigDecimal("50.00")));
        org.assertj.core.api.Assertions.assertThat(bob.getBalance())
                .isEqualByComparingTo(bobStartingBalance.subtract(new BigDecimal("25.25")));

        mockMvc.perform(get("/games/" + createdMatch.getId()).session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("won on the time tiebreak.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Solution:")));

        mockMvc.perform(get("/games").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Alice won on the time tiebreak."))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No live matches for this filter")));
    }

    @Test
    void wordleEntryFeeAppearsInPortfolioDonationTotal() throws Exception {
        MvcResult sessionResult = mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) sessionResult.getRequest().getSession(false);

        mockMvc.perform(post("/games")
                        .session(session)
                        .param("gameType", "WORDLE")
                        .param("betAmount", "25.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games"));

        mockMvc.perform(get("/api/users/1/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDonated").value(0.25));
    }

    @Test
    void creatorCanCancelWaitingMatchAndRestoreBalance() throws Exception {
        BigDecimal startingBalance = userRepository.findById(1L).orElseThrow().getBalance();

        MvcResult sessionResult = mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) sessionResult.getRequest().getSession(false);

        mockMvc.perform(post("/games")
                        .session(session)
                        .param("gameType", "WORDLE")
                        .param("betAmount", "25.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games"));

        WordleMatch createdMatch = wordleMatchRepository.findAll().stream()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/games/" + createdMatch.getId() + "/cancel")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/games"));

        org.assertj.core.api.Assertions.assertThat(wordleMatchRepository.findById(createdMatch.getId())).isEmpty();
        org.assertj.core.api.Assertions.assertThat(userRepository.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo(startingBalance);
    }
}
