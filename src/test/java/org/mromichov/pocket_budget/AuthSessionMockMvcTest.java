package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSessionMockMvcTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void homeAndAssetsArePublic() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
        mvc.perform(get("/styles.css")).andExpect(status().isOk());
        mvc.perform(get("/app.js")).andExpect(status().isOk());
    }

    @Test
    void anonymousMeReturns401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void csrfEndpointReturnsRealTokenAndSessionWithoutCaching() throws Exception {
        var result = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        var token = mapper.readTree(result.getResponse().getContentAsString());

        // No csrf() test helper: send the token returned by the real endpoint.
        // Invalid DTO prevents creating a user while proving CSRF accepted the header.
        mvc.perform(post("/api/auth/register").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
