package org.mromichov.pocket_budget;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mromichov.pocket_budget.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationMockMvcTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsersRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;

    private final List<String> testUsernames = new ArrayList<>();
    private boolean testDatabase;

    @BeforeEach
    void verifyDatabase() {
        assertEquals("pocket_budget_test", jdbc.queryForObject("SELECT current_database()", String.class));
        testDatabase = true;
    }

    @AfterEach
    void cleanup() {
        if (testDatabase) {
            for (String username : testUsernames) {
                jdbc.update("DELETE FROM users WHERE username = ?", username);
            }
        }
    }

    @Test
    void registrationStoresHashAndAllowsRealLogin() throws Exception {
        String username = username();
        String password = "test-password";
        var registration = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType("application/json").content(body(username, password)))
                .andExpect(status().isCreated())
                .andExpect(unauthenticated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        var saved = users.findByUsername(username).orElseThrow();
        assertTrue(saved.isEnabled());
        assertNotEquals(password, saved.getPasswordHash());
        assertTrue(encoder.matches(password, saved.getPasswordHash()));
        assertEquals(saved.getId().longValue(), mapper.readTree(
                registration.getResponse().getContentAsString()).get("id").asLong());

        var login = mvc.perform(formLogin().user(username).password(password))
                .andExpect(authenticated().withUsername(username))
                .andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/expenses").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void duplicateUsernameReturns409AndKeepsOriginalPassword() throws Exception {
        String username = username();
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType("application/json").content(body(username, "first-password")))
                .andExpect(status().isCreated());
        Long originalId = users.findByUsername(username).orElseThrow().getId();

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType("application/json").content(body(username, "second-password")))
                .andExpect(status().isConflict());

        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE username = ?", Long.class, username));
        var saved = users.findByUsername(username).orElseThrow();
        assertEquals(originalId, saved.getId());
        assertTrue(encoder.matches("first-password", saved.getPasswordHash()));
        assertFalse(encoder.matches("second-password", saved.getPasswordHash()));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void invalidRegistrationReturns400WithoutSaving(String invalidBody) throws Exception {
        Long before = jdbc.queryForObject("SELECT count(*) FROM users", Long.class);
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType("application/json").content(invalidBody))
                .andExpect(status().isBadRequest());
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM users", Long.class));
    }

    static Stream<String> invalidBodies() {
        return Stream.of(
                "{}", 
                "{\"username\":null,\"password\":\"test-password\"}",
                "{\"username\":\"\",\"password\":\"test-password\"}",
                "{\"username\":\"ab\",\"password\":\"test-password\"}",
                "{\"username\":\"Bad_Name\",\"password\":\"test-password\"}",
                "{\"username\":\"with space\",\"password\":\"test-password\"}",
                "{\"username\":\"" + "a".repeat(51) + "\",\"password\":\"test-password\"}",
                "{\"username\":\"valid_name\",\"password\":null}",
                "{\"username\":\"valid_name\",\"password\":\"        \"}",
                "{\"username\":\"valid_name\",\"password\":\"short\"}",
                "{\"username\":\"valid_name\",\"password\":\"" + "a".repeat(65) + "\"}"
        );
    }

    @Test
    void missingCsrfReturns403WithoutSaving() throws Exception {
        String username = username();
        mvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(body(username, "test-password")))
                .andExpect(status().isForbidden());
        assertThat(users.findByUsername(username)).isEmpty();
    }

    @Test
    void invalidCsrfReturns403WithoutSaving() throws Exception {
        String username = username();
        mvc.perform(post("/api/auth/register").with(csrf().useInvalidToken())
                        .contentType("application/json").content(body(username, "test-password")))
                .andExpect(status().isForbidden());
        assertThat(users.findByUsername(username)).isEmpty();
    }

    private String username() {
        String username = "reg_" + UUID.randomUUID().toString().replace("-", "");
        testUsernames.add(username);
        return username;
    }

    private String body(String username, String password) {
        return mapper.writeValueAsString(Map.of("username", username, "password", password));
    }
}
