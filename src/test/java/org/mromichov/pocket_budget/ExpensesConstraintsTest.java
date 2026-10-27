package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ExpensesConstraintsTest {

    private static final String INSERT = """
            INSERT INTO expenses (amount, category, spent_at)
            VALUES (?, ?, ?)
            """;

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void databaseRejectsInvalidAmount(
            Long amount,
            @Autowired JdbcTemplate template
    ) {
        assertTestDatabase(template);

        assertThrows(DataIntegrityViolationException.class,
                () -> template.update(
                        INSERT, amount, "food", LocalDate.of(2026, 10, 22)
                ));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t", "\n", " \t\n"})
    void databaseRejectsBlankCategory(
            String category,
            @Autowired JdbcTemplate template
    ) {
        assertTestDatabase(template);

        assertThrows(DataIntegrityViolationException.class,
                () -> template.update(
                        INSERT, 300L, category, LocalDate.of(2026, 10, 22)
                ));
    }

    @Test
    void databaseRejectsMissingDate(@Autowired JdbcTemplate template) {
        assertTestDatabase(template);

        assertThrows(DataIntegrityViolationException.class,
                () -> template.update(INSERT, 300L, "food", null));
    }

    private void assertTestDatabase(JdbcTemplate template) {
        assertEquals(
                "pocket_budget_test",
                template.queryForObject(
                        "SELECT current_database()", String.class
                )
        );
    }
}
