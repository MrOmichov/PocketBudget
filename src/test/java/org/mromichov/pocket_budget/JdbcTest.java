package org.mromichov.pocket_budget;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class JdbcTest {
    @Test
    void checkDatabaseAndUser(@Autowired JdbcTemplate template) {
        String sql = "SELECT current_database();";
        String currentDb = template.queryForObject(sql, String.class);
        assertEquals("pocket_budget_test", currentDb);

        sql = "SELECT current_user;";
        String currentUser = template.queryForObject(sql, String.class);
        assertEquals("pocket_budget", currentUser);
    }
}
