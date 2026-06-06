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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.auth.mode=INVITE")
@AutoConfigureMockMvc
class InviteAccessIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void inviteModeUsesUsernameOnlyAndHidesUserSwitcher() throws Exception {
        mockMvc.perform(get("/markets"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access"));

        MvcResult accessResult = mockMvc.perform(post("/access")
                        .param("username", "Roni"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/markets"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) accessResult.getRequest().getSession(false);

        mockMvc.perform(get("/markets").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Roni")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Log out")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Switch"))));
    }
}
