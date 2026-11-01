package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.mromichov.pocket_budget.users.BudgetUserDetails;
import org.mromichov.pocket_budget.users.UserEntity;
import org.mromichov.pocket_budget.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthenticationMockMvcTest {
    @Test
    void authenticationSucceeds(
            @Autowired
            MockMvc mvc,
            @Autowired
            UsersRepository usersRepository,
            @Autowired
            PasswordEncoder passwordEncoder,
            @Autowired
            JdbcTemplate template
    ) throws Exception {
        String sql = "SELECT current_database();";
        String currentDb = template.queryForObject(sql, String.class);
        assertEquals("pocket_budget_test", currentDb);

        String username = "test_" + UUID.randomUUID().toString().replace("-", "");
        String rawPassword = "password";
        UserEntity savedUser = usersRepository.saveAndFlush(new UserEntity(username, passwordEncoder.encode(rawPassword), true));

        MvcResult loginResult = mvc.perform(
                        formLogin()
                                .user(username)
                                .password(rawPassword)
                )
                .andExpect(authenticated().withUsername(username))
                .andExpect(authenticated().withAuthentication(authentication -> {
                    BudgetUserDetails principal = assertInstanceOf(
                            BudgetUserDetails.class,
                            authentication.getPrincipal()
                    );
                    assertEquals(savedUser.getId().longValue(), principal.getId());
                }))
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mvc.perform(get("/api/expenses").session(session))
                .andExpect(authenticated().withUsername(username))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordRejectsLogin(@Autowired MockMvc mvc,
                                   @Autowired UsersRepository users,
                                   @Autowired PasswordEncoder encoder,
                                   @Autowired JdbcTemplate jdbc) throws Exception {
        assertEquals("pocket_budget_test", jdbc.queryForObject("SELECT current_database()", String.class));
        String username = "test_" + UUID.randomUUID().toString().replace("-", "");
        users.saveAndFlush(new UserEntity(username, encoder.encode("correct-password"), true));

        MvcResult result = mvc.perform(formLogin().user(username).password("wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        var request = get("/api/expenses");
        if (session != null) {
            request.session(session);
        }
        mvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUserCannotLoginWithCorrectPassword(@Autowired MockMvc mvc,
                                                    @Autowired UsersRepository users,
                                                    @Autowired PasswordEncoder encoder,
                                                    @Autowired JdbcTemplate jdbc) throws Exception {
        assertEquals("pocket_budget_test", jdbc.queryForObject("SELECT current_database()", String.class));
        String username = "test_" + UUID.randomUUID().toString().replace("-", "");
        users.saveAndFlush(new UserEntity(username, encoder.encode("correct-password"), false));

        MvcResult result = mvc.perform(formLogin().user(username).password("correct-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        var request = get("/api/expenses");
        if (session != null) {
            request.session(session);
        }
        mvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesAuthenticatedSession(@Autowired MockMvc mvc,
                                               @Autowired UsersRepository users,
                                               @Autowired PasswordEncoder encoder,
                                               @Autowired JdbcTemplate jdbc) throws Exception {
        assertEquals("pocket_budget_test", jdbc.queryForObject("SELECT current_database()", String.class));
        String username = "test_" + UUID.randomUUID().toString().replace("-", "");
        users.saveAndFlush(new UserEntity(username, encoder.encode("password"), true));

        MvcResult login = mvc.perform(formLogin().user(username).password("password"))
                .andExpect(authenticated().withUsername(username))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        String oldSessionId = session.getId();
        mvc.perform(get("/api/expenses").session(session))
                .andExpect(status().isOk());

        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?logout"));
        assertTrue(session.isInvalid());

        // MockMvc does not resolve session cookies through a real servlet container.
        // Check session invalidation above, then an anonymous request below.
        mvc.perform(get("/api/expenses")
                        .cookie(new jakarta.servlet.http.Cookie("JSESSIONID", oldSessionId)))
                .andExpect(status().isUnauthorized());
    }
}
