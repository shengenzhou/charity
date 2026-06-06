package com.example.charitymarket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.auth.mode=INVITE",
        "app.auth.alice-token=alice-hackathon-token",
        "app.auth.bob-token=bob-hackathon-token"
})
@AutoConfigureMockMvc
class InviteAccessIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void inviteModeRequiresAccessTokenAndHidesUserSwitcher() throws Exception {
        mockMvc.perform(get("/markets"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access"));

        MvcResult accessResult = mockMvc.perform(get("/access").param("token", "alice-hackathon-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/markets"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) accessResult.getRequest().getSession(false);

        mockMvc.perform(get("/markets").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alice")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Log out")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Switch"))));
    }
}
