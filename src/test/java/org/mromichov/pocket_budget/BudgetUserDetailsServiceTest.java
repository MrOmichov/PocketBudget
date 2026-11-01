package org.mromichov.pocket_budget;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mromichov.pocket_budget.users.BudgetUserDetails;
import org.mromichov.pocket_budget.users.BudgetUserDetailsService;
import org.mromichov.pocket_budget.users.UserEntity;
import org.mromichov.pocket_budget.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BudgetUserDetailsServiceTest {
    @Test
    void userCorrectlyLoadsFromDatabase(
            @Autowired
            UsersRepository usersRepository,

            @Autowired
            BudgetUserDetailsService budgetUserDetailsService,

            @Autowired
            PasswordEncoder passwordEncoder,

            @Autowired
            JdbcTemplate template,

            @Autowired
            EntityManager entityManager
    ) {
        String sql = "SELECT current_database();";
        String currentDb = template.queryForObject(sql, String.class);
        assertEquals("pocket_budget_test", currentDb);

        String username = "test_" + UUID.randomUUID().toString().replace("-", "");
        String rawPassword = "password";
        String passwordHash = passwordEncoder.encode(rawPassword);
        UserEntity user = new UserEntity(
                username,
                passwordHash,
                true
        );

        UserEntity savedUser = usersRepository.save(user);

        usersRepository.flush();
        entityManager.clear();

        BudgetUserDetails loadedUserDetails = (BudgetUserDetails) budgetUserDetailsService.loadUserByUsername(username);

        assertEquals(savedUser.getUsername(), loadedUserDetails.getUsername());
        assertEquals(savedUser.getPasswordHash(), loadedUserDetails.getPassword());
        assertTrue(loadedUserDetails.isEnabled());
        assertTrue(passwordEncoder.matches(rawPassword, loadedUserDetails.getPassword()));
        assertFalse(passwordEncoder.matches("wrong-password", loadedUserDetails.getPassword()));

        assertEquals(savedUser.getId().longValue(), loadedUserDetails.getId());
    }

    @Test
    void unknownUsernameThrowsNotFoundException(
            @Autowired
            BudgetUserDetailsService budgetUserDetailsService
    ) {
        String unknownUsername = "missing_" + UUID.randomUUID().toString().replace("-", "");
        assertThrows(UsernameNotFoundException.class, () -> budgetUserDetailsService.loadUserByUsername(unknownUsername));
    }
}
