package org.mromichov.pocket_budget;

import org.junit.jupiter.api.BeforeEach;
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
    private static final String INSERT =
            "INSERT INTO expenses(amount, category_id, spent_at, user_id) VALUES (?, ?, ?, ?)";
    @Autowired private JdbcTemplate jdbc;
    private Long categoryId;
    private Long userId;

    @BeforeEach
    void prepareDatabase() {
        assertEquals("pocket_budget_test", jdbc.queryForObject("SELECT current_database()", String.class));
        jdbc.update("DELETE FROM expenses");
        userId = jdbc.queryForObject("INSERT INTO users(username,password_hash,enabled) VALUES (?, ?, true) RETURNING id",
                Long.class, "test_" + java.util.UUID.randomUUID().toString().replace("-", ""), "!test");
        jdbc.update("DELETE FROM categories");
        categoryId = jdbc.queryForObject(
                "INSERT INTO categories(name) VALUES (?) RETURNING id", Long.class, "food");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void databaseRejectsInvalidAmount(Long amount) {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT, amount, categoryId, LocalDate.of(2026, 10, 22), userId));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t", "\n", " \t\n"})
    void databaseRejectsBlankCategoryName(String name) {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO categories(name) VALUES (?)", name));
    }

    @Test
    void databaseRejectsDuplicateCategoryName() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO categories(name) VALUES (?)", "food"));
    }

    @Test
    void databaseRejectsMissingDate() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT, 300L, categoryId, null, userId));
    }

    @Test
    void databaseRejectsMissingCategory() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT, 300L, null, LocalDate.of(2026, 10, 22), userId));
    }

    @Test
    void databaseRejectsUnknownCategory() {
        Long unknown = jdbc.queryForObject(
                "INSERT INTO categories(name) VALUES (?) RETURNING id", Long.class, "removed");
        jdbc.update("DELETE FROM categories WHERE id = ?", unknown);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT, 300L, unknown, LocalDate.of(2026, 10, 22), userId));
    }

    @Test
    void databaseRejectsDeletingUsedCategory() {
        jdbc.update(INSERT, 300L, categoryId, LocalDate.of(2026, 10, 22), userId);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("DELETE FROM categories WHERE id = ?", categoryId));
    }

    @Test
    void databaseRejectsMissingOwner() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT, 300L, categoryId, LocalDate.of(2026, 10, 22), null));
    }

    @Test
    void databaseRejectsUnknownOwner() {
        Long removed = jdbc.queryForObject(
                "INSERT INTO users(username,password_hash,enabled) VALUES (?, ?, true) RETURNING id",
                Long.class, "gone_" + java.util.UUID.randomUUID().toString().replace("-", ""), "!test");
        jdbc.update("DELETE FROM users WHERE id = ?", removed);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT, 300L, categoryId, LocalDate.of(2026, 10, 22), removed));
    }
}
