package org.mromichov.pocket_budget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mromichov.pocket_budget.expenses.ExpensesPageResponse;
import org.mromichov.pocket_budget.users.BudgetUserDetails;
import org.mromichov.pocket_budget.users.UserEntity;
import org.mromichov.pocket_budget.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpensesFiltersTest {
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
        var owner = users.saveAndFlush(new UserEntity(username(), "!test", true));
        var other = users.saveAndFlush(new UserEntity(username(), "!test", true));
        principal = new BudgetUserDetails(owner);

        // Amounts identify fixtures, independently of identity sequence values.
        expense(owner.getId(), food, 100, "2026-10-09");
        expense(owner.getId(), food, 200, "2026-10-10");
        expense(owner.getId(), transport, 300, "2026-10-15");
        expense(owner.getId(), food, 400, "2026-10-20");
        expense(owner.getId(), food, 500, "2026-10-21");
        expense(other.getId(), food, 999, "2026-10-15");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "ALL|ALL|ALL|500;400;300;200;100",
            "2026-10-10|ALL|ALL|500;400;300;200",
            "ALL|2026-10-20|ALL|400;300;200;100",
            "2026-10-10|2026-10-20|ALL|400;300;200",
            "2026-10-10|2026-10-10|ALL|200",
            "ALL|ALL|food|500;400;200;100",
            "2026-10-10|ALL|food|500;400;200",
            "ALL|2026-10-20|food|400;200;100",
            "2026-10-10|2026-10-20|food|400;200",
            "2026-10-10|2026-10-20|transport|300"
    }, delimiter = '|')
    void filtersAreInclusiveAndCombined(String from, String to, String category, String expected) throws Exception {
        var request = filteredRequest(from, to, category);
        ExpensesPageResponse result = read(request);
        assertThat(result.items().stream().map(e -> e.amount().longValueExact()).toList())
                .containsExactlyElementsOf(Arrays.stream(expected.split(";")).map(Long::valueOf).toList());
        assertEquals(expected.split(";").length, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void filteringHappensBeforePaginationAndCountsOnlyOwner() throws Exception {
        ExpensesPageResponse first = read(filteredRequest("2026-10-10", "2026-10-20", "food")
                .param("size", "1").param("page", "0"));
        ExpensesPageResponse second = read(filteredRequest("2026-10-10", "2026-10-20", "food")
                .param("size", "1").param("page", "1"));
        assertEquals(400L, first.items().get(0).amount().longValueExact());
        assertEquals(200L, second.items().get(0).amount().longValueExact());
        assertEquals(2L, first.totalElements());
        assertEquals(2L, second.totalElements());
        assertEquals(2, first.totalPages());
        assertEquals(2, second.totalPages());
        assertEquals(0, first.page());
        assertEquals(1, second.page());
        assertEquals(1, first.size());
    }

    @Test
    void noMatchesReturnEmptyPage() throws Exception {
        ExpensesPageResponse result = read(filteredRequest("2026-11-01", "ALL", "ALL"));
        assertThat(result.items()).isEmpty();
        assertEquals(0L, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void unknownCategoryReturnsEmptyPage() throws Exception {
        long unknown = category("removed");
        jdbc.update("DELETE FROM categories WHERE id = ?", unknown);
        ExpensesPageResponse result = read(get("/api/expenses")
                .param("categoryId", Long.toString(unknown)));
        assertThat(result.items()).isEmpty();
        assertEquals(0L, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void reversedPeriodReturns400() throws Exception {
        mvc.perform(filteredRequest("2026-10-20", "2026-10-10", "ALL").with(user(principal)))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder filteredRequest(String from, String to, String category) {
        var request = get("/api/expenses");
        if (!from.equals("ALL")) request.param("from", from);
        if (!to.equals("ALL")) request.param("to", to);
        if (!category.equals("ALL")) {
            request.param("categoryId", Long.toString(category.equals("food") ? food : transport));
        }
        return request;
    }

    private ExpensesPageResponse read(MockHttpServletRequestBuilder request) throws Exception {
        var response = mvc.perform(request.with(user(principal)))
                .andExpect(status().isOk()).andReturn().getResponse();
        return mapper.readValue(response.getContentAsString(), ExpensesPageResponse.class);
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
