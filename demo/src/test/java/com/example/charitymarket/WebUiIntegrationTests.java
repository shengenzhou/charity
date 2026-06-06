package com.example.charitymarket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WebUiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

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
}
