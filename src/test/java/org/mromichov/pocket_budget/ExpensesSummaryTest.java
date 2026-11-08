package org.mromichov.pocket_budget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mromichov.pocket_budget.expenses.responses.CategoryExpenseSummary;
import org.mromichov.pocket_budget.expenses.responses.ExpensesSummaryResponse;
import org.mromichov.pocket_budget.users.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpensesSummaryTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsersRepository users;
    @Autowired ObjectMapper mapper;
    private BudgetUserDetails principal;
    private long food;
    private long transport;

    @BeforeEach
    void prepare() {
        assertEquals("pocket_budget_test", jdbc.queryForObject("SELECT current_database()", String.class));
        jdbc.update("DELETE FROM expenses");
        jdbc.update("DELETE FROM categories");
        food = category("food");
        transport = category("transport");
        category("unused");
        var owner = users.saveAndFlush(new UserEntity(username(), "!test", true));
        var other = users.saveAndFlush(new UserEntity(username(), "!test", true));
        principal = new BudgetUserDetails(owner);
        expense(owner.getId(), food, 100, "2026-10-09");
        expense(owner.getId(), food, 200, "2026-10-10");
        expense(owner.getId(), transport, 300, "2026-10-15");
        expense(owner.getId(), food, 400, "2026-10-20");
        expense(owner.getId(), food, 500, "2026-10-21");
        expense(other.getId(), food, 999, "2026-10-15");
    }

    @Test
    void groupsAndTotalsIncludeOnlyCurrentUsersExpenses() throws Exception {
        var result = read(get("/api/expenses/summary"));
        assertEquals(BigInteger.valueOf(1500), result.totalAmount());
        assertEquals(5L, result.expenseCount());
        assertThat(result.categories()).containsExactly(
                new CategoryExpenseSummary(food, "food", BigInteger.valueOf(1200), 4L),
                new CategoryExpenseSummary(transport, "transport", BigInteger.valueOf(300), 1L));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "2026-10-10|ALL|ALL|1400|4",
            "ALL|2026-10-20|ALL|1000|4",
            "2026-10-10|2026-10-20|ALL|900|3",
            "2026-10-10|2026-10-10|ALL|200|1",
            "ALL|ALL|food|1200|4",
            "2026-10-10|ALL|food|1100|3",
            "ALL|2026-10-20|food|700|3",
            "2026-10-10|2026-10-20|food|600|2",
            "2026-10-10|2026-10-20|transport|300|1"
    }, delimiter = '|')
    void filtersAreInclusiveAndCombined(String from, String to, String category,
                                        long amount, long count) throws Exception {
        var request = get("/api/expenses/summary");
        if (!from.equals("ALL")) request.param("from", from);
        if (!to.equals("ALL")) request.param("to", to);
        if (!category.equals("ALL")) {
            request.param("categoryId", Long.toString(category.equals("food") ? food : transport));
        }
        var result = read(request);
        assertEquals(BigInteger.valueOf(amount), result.totalAmount());
        assertEquals(count, result.expenseCount());
        assertEquals(result.totalAmount(), result.categories().stream()
                .map(CategoryExpenseSummary::totalAmount).reduce(BigInteger.ZERO, BigInteger::add));
        assertEquals(count, result.categories().stream().mapToLong(CategoryExpenseSummary::expenseCount).sum());
        if (!category.equals("ALL")) {
            assertEquals(1, result.categories().size());
            assertEquals(category, result.categories().get(0).category());
        }
    }

    @Test
    void noMatchesReturnZerosAndEmptyCategories() throws Exception {
        var result = read(get("/api/expenses/summary").param("from", "2026-11-01"));
        assertEquals(BigInteger.ZERO, result.totalAmount());
        assertEquals(0L, result.expenseCount());
        assertThat(result.categories()).isEmpty();
    }

    @Test
    void unknownCategoryReturnsEmptySummary() throws Exception {
        long unknown = category("removed");
        jdbc.update("DELETE FROM categories WHERE id = ?", unknown);
        var result = read(get("/api/expenses/summary").param("categoryId", Long.toString(unknown)));
        assertEquals(BigInteger.ZERO, result.totalAmount());
        assertEquals(0L, result.expenseCount());
        assertThat(result.categories()).isEmpty();
    }

    @Test
    void sumsCanExceedLongRangeWithinAndAcrossCategories() throws Exception {
        jdbc.update("DELETE FROM expenses");
        expense(principal.getId(), food, Long.MAX_VALUE, "2026-10-10");
        expense(principal.getId(), food, Long.MAX_VALUE, "2026-10-10");
        expense(principal.getId(), transport, 1L, "2026-10-10");
        var result = read(get("/api/expenses/summary"));
        BigInteger foodTotal = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO);
        assertEquals(foodTotal.add(BigInteger.ONE), result.totalAmount());
        assertEquals(3L, result.expenseCount());
        assertThat(result.categories()).containsExactly(
                new CategoryExpenseSummary(food, "food", foodTotal, 2L),
                new CategoryExpenseSummary(transport, "transport", BigInteger.ONE, 1L));
    }

    @Test
    void summaryIsNotLimitedToListPage() throws Exception {
        mvc.perform(get("/api/expenses").with(user(principal)).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
        var result = read(get("/api/expenses/summary"));
        assertEquals(BigInteger.valueOf(1500), result.totalAmount());
        assertEquals(5L, result.expenseCount());
    }

    @ParameterizedTest
    @CsvSource({"from,invalid", "to,2026-02-30", "categoryId,0", "categoryId,-1", "categoryId,abc"})
    void invalidParametersReturn400(String name, String value) throws Exception {
        mvc.perform(get("/api/expenses/summary").with(user(principal)).param(name, value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reversedDatesReturn400() throws Exception {
        mvc.perform(get("/api/expenses/summary").with(user(principal))
                        .param("from", "2026-10-20").param("to", "2026-10-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousSummaryReturns401() throws Exception {
        mvc.perform(get("/api/expenses/summary")).andExpect(status().isUnauthorized());
    }

    private ExpensesSummaryResponse read(MockHttpServletRequestBuilder request) throws Exception {
        var response = mvc.perform(request.with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json")).andReturn().getResponse();
        return mapper.readValue(response.getContentAsString(), ExpensesSummaryResponse.class);
    }

    private long category(String name) {
        return jdbc.queryForObject("INSERT INTO categories(name) VALUES (?) RETURNING id", Long.class, name);
    }

    private void expense(long owner, long category, long amount, String date) {
        jdbc.update("INSERT INTO expenses(user_id, category_id, amount, spent_at) VALUES (?, ?, ?, ?)",
                owner, category, amount, LocalDate.parse(date));
    }

    private String username() {
        return "test_" + UUID.randomUUID().toString().replace("-", "");
    }
}
