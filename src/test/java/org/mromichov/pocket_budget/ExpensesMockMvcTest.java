package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.mromichov.pocket_budget.expenses.Expense;
import org.mromichov.pocket_budget.expenses.ExpensesPageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ExpensesMockMvcTest {
    @Test
    void postAndGetNewExpense(@Autowired MockMvc mvc, @Autowired ObjectMapper objectMapper) throws Exception {
        MvcResult postResult = mvc.perform(post("/api/expenses").contentType("application/json").content(
                        """
                        {
                            "amount": 300,
                            "category": "food",
                            "spentAt": "2026-10-19"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json"))
                .andReturn();

        Expense createdExpense = objectMapper.readValue(postResult.getResponse().getContentAsString(), Expense.class);
        assertEquals(new BigInteger("300"), createdExpense.amount());
        assertEquals("food", createdExpense.category());
        assertEquals(LocalDate.of(2026, 10, 19), createdExpense.spentAt());

        mvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(createdExpense.id()))
                .andExpect(jsonPath("$.items[0].amount").value(300))
                .andExpect(jsonPath("$.items[0].category").value("food"))
                .andExpect(jsonPath("$.items[0].spentAt").value("2026-10-19"));
    }

    @BeforeEach
    void prepareDatabase(@Autowired JdbcTemplate jdbc) {
        assertEquals("pocket_budget_test", jdbc.queryForObject("SELECT current_database()", String.class));
        // DELETE is rolled back with the test. Identity values are not reset.
        jdbc.update("DELETE FROM expenses");
    }

    @ParameterizedTest
    @MethodSource("invalidExpenses")
    void invalidPostDoesNotSaveExpense(String body, @Autowired MockMvc mvc,
                                      @Autowired JdbcTemplate jdbc) throws Exception {
        mvc.perform(post("/api/expenses").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expenses", Long.class)).isZero();
    }

    static Stream<String> invalidExpenses() {
        Stream<String> amounts = Stream.of("-1", "0", "null", "9223372036854775808")
                .map(amount -> """
                        {"amount":%s,"category":"food","spentAt":"2026-10-19"}
                        """.formatted(amount));
        Stream<String> categories = Stream.of("null", "\"\"", "\" \"", "\"\\t\"", "\"\\n\"")
                .map(category -> """
                        {"amount":300,"category":%s,"spentAt":"2026-10-19"}
                        """.formatted(category));
        return Stream.concat(Stream.concat(amounts, categories), Stream.of(
                """
                {"category":"food","spentAt":"2026-10-19"}
                """,
                """
                {"amount":300,"spentAt":"2026-10-19"}
                """,
                """
                {"amount":300,"category":"food"}
                """,
                """
                {"amount":300,"category":"food","spentAt":null}
                """));
    }
    @Test
    void pagesSortByDateThenId(@Autowired MockMvc mvc, @Autowired ObjectMapper mapper) throws Exception {
        Expense older = create(mvc, mapper, "2026-10-19");
        Expense newer = create(mvc, mapper, "2026-10-27");
        Expense sameDate = create(mvc, mapper, "2026-10-27");
        Expense oldest = create(mvc, mapper, "2026-10-01");
        assertThat(sameDate.id()).isGreaterThan(newer.id());
        Expense[] expected = {sameDate, newer, older, oldest};
        for (int i = 0; i < expected.length; i++) {
            ExpensesPageResponse page = readPage(mvc, mapper, i, 1);
            assertThat(page.items()).containsExactly(expected[i]);
            assertMetadata(page, i, 1, 4, 4);
        }
    }

    @Test
    void partialLastPageAndBeyondKeepMetadata(@Autowired MockMvc mvc,
                                             @Autowired ObjectMapper mapper) throws Exception {
        Expense oldest = create(mvc, mapper, "2026-10-19");
        create(mvc, mapper, "2026-10-20");
        create(mvc, mapper, "2026-10-21");
        ExpensesPageResponse last = readPage(mvc, mapper, 1, 2);
        assertThat(last.items()).containsExactly(oldest);
        assertMetadata(last, 1, 2, 3, 2);
        ExpensesPageResponse beyond = readPage(mvc, mapper, 2, 2);
        assertThat(beyond.items()).isEmpty();
        assertMetadata(beyond, 2, 2, 3, 2);
    }

    @Test
    void emptyDatabaseReturnsEmptyPage(@Autowired MockMvc mvc,
                                      @Autowired ObjectMapper mapper) throws Exception {
        ExpensesPageResponse page = readPage(mvc, mapper, 0, 2);
        assertThat(page.items()).isEmpty();
        assertMetadata(page, 0, 2, 0, 0);
    }

    private Expense create(MockMvc mvc, ObjectMapper mapper, String date) throws Exception {
        String body = """
                {"amount":300,"category":"food","spentAt":"%s"}
                """.formatted(date);
        var response = mvc.perform(post("/api/expenses").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json")).andReturn().getResponse();
        Expense expense = mapper.readValue(response.getContentAsString(), Expense.class);
        assertEquals("/api/expenses/" + expense.id(), response.getHeader("Location"));
        return expense;
    }

    private ExpensesPageResponse readPage(MockMvc mvc, ObjectMapper mapper, int page, int size) throws Exception {
        var response = mvc.perform(get("/api/expenses")
                        .param("page", Integer.toString(page)).param("size", Integer.toString(size)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json")).andReturn().getResponse();
        return mapper.readValue(response.getContentAsString(), ExpensesPageResponse.class);
    }

    private void assertMetadata(ExpensesPageResponse page, int number, int size, long total, int pages) {
        assertEquals(number, page.page());
        assertEquals(size, page.size());
        assertEquals(total, page.totalElements());
        assertEquals(pages, page.totalPages());
    }
}
