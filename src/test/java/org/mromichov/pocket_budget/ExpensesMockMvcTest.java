package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.mromichov.pocket_budget.users.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ExpensesMockMvcTest {
    private Long categoryId;
    private BudgetUserDetails principal;

    @Test
    void postAndGetNewExpense(@Autowired MockMvc mvc, @Autowired ObjectMapper objectMapper) throws Exception {
        MvcResult postResult = mvc.perform(post("/api/expenses").with(user(principal)).with(csrf()).contentType("application/json").content(
                        """
                        {
                            "amount": 300,
                            "categoryId": %d,
                            "spentAt": "2026-10-19"
                        }
                        """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json"))
                .andReturn();

        Expense createdExpense = objectMapper.readValue(postResult.getResponse().getContentAsString(), Expense.class);
        assertEquals(new BigInteger("300"), createdExpense.amount());
        assertEquals("food", createdExpense.category());
        assertEquals(categoryId.longValue(), createdExpense.categoryId());
        assertEquals(LocalDate.of(2026, 10, 19), createdExpense.spentAt());

        mvc.perform(get("/api/expenses").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(createdExpense.id()))
                .andExpect(jsonPath("$.items[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$.items[0].amount").value(300))
                .andExpect(jsonPath("$.items[0].category").value("food"))
                .andExpect(jsonPath("$.items[0].spentAt").value("2026-10-19"));
    }

    @BeforeEach
    void prepareDatabase(@Autowired JdbcTemplate jdbc, @Autowired UsersRepository users) {
        assertEquals("pocket_budget_test", jdbc.queryForObject("SELECT current_database()", String.class));
        // DELETE is rolled back with the test. Identity values are not reset.
        jdbc.update("DELETE FROM expenses");
        var owner = users.save(new UserEntity("test_" + java.util.UUID.randomUUID().toString().replace("-", ""), "!test", true));
        principal = new BudgetUserDetails(owner);
        jdbc.update("DELETE FROM categories");
        categoryId = jdbc.queryForObject(
                "INSERT INTO categories(name) VALUES (?) RETURNING id", Long.class, "food");
    }

    @ParameterizedTest
    @MethodSource("invalidExpenses")
    void invalidPostDoesNotSaveExpense(String body, @Autowired MockMvc mvc,
                                      @Autowired JdbcTemplate jdbc) throws Exception {
        mvc.perform(post("/api/expenses").with(user(principal)).with(csrf()).contentType("application/json").content(body.replace("CATEGORY_ID", categoryId.toString())))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expenses", Long.class)).isZero();
    }

    static Stream<String> invalidExpenses() {
        Stream<String> amounts = Stream.of("-1", "0", "null", "9223372036854775808")
                .map(amount -> """
                        {"amount":%s,"categoryId":CATEGORY_ID,"spentAt":"2026-10-19"}
                        """.formatted(amount));
        Stream<String> categories = Stream.of("null", "0", "-1")
                .map(category -> """
                        {"amount":300,"categoryId":%s,"spentAt":"2026-10-19"}
                        """.formatted(category));
        return Stream.concat(Stream.concat(amounts, categories), Stream.of(
                """
                {"categoryId":CATEGORY_ID,"spentAt":"2026-10-19"}
                """,
                """
                {"amount":300,"spentAt":"2026-10-19"}
                """,
                """
                {"amount":300,"categoryId":CATEGORY_ID}
                """,
                """
                {"amount":300,"categoryId":CATEGORY_ID,"spentAt":null}
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
                {"amount":300,"categoryId":CATEGORY_ID,"spentAt":"%s"}
                """.formatted(date).replace("CATEGORY_ID", categoryId.toString());
        var response = mvc.perform(post("/api/expenses").with(user(principal)).with(csrf()).contentType("application/json").content(body.replace("CATEGORY_ID", categoryId.toString())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json")).andReturn().getResponse();
        Expense expense = mapper.readValue(response.getContentAsString(), Expense.class);
        assertEquals("/api/expenses/" + expense.id(), response.getHeader("Location"));
        return expense;
    }

    private ExpensesPageResponse readPage(MockMvc mvc, ObjectMapper mapper, int page, int size) throws Exception {
        var response = mvc.perform(get("/api/expenses").with(user(principal))
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

    @Test
    void categoriesIncludeUnusedCategoriesInIdOrder(@Autowired MockMvc mvc,
                                                     @Autowired JdbcTemplate jdbc) throws Exception {
        Long unused = jdbc.queryForObject(
                "INSERT INTO categories(name) VALUES (?) RETURNING id", Long.class, "unused");
        mvc.perform(get("/api/categories").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(categoryId))
                .andExpect(jsonPath("$[0].name").value("food"))
                .andExpect(jsonPath("$[1].id").value(unused))
                .andExpect(jsonPath("$[1].name").value("unused"));
    }

    @Test
    void unknownCategoryDoesNotSaveExpense(@Autowired MockMvc mvc,
                                           @Autowired JdbcTemplate jdbc) throws Exception {
        Long unknown = jdbc.queryForObject(
                "INSERT INTO categories(name) VALUES (?) RETURNING id", Long.class, "removed");
        jdbc.update("DELETE FROM categories WHERE id = ?", unknown);
        Long before = jdbc.queryForObject("SELECT count(*) FROM expenses", Long.class);
        mvc.perform(post("/api/expenses").with(user(principal)).with(csrf()).contentType("application/json").content("""
                {"amount":300,"categoryId":%d,"spentAt":"2026-10-19"}
                """.formatted(unknown)))
                .andExpect(status().isNotFound());
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM expenses", Long.class));
    }

    @Test
    void postWithoutCsrfIsRejected(@Autowired MockMvc mvc, @Autowired JdbcTemplate jdbc) throws Exception {
        mvc.perform(post("/api/expenses").with(user(principal))
                        .contentType("application/json").content("""
                        {"amount":300,"categoryId":%d,"spentAt":"2026-10-19"}
                        """.formatted(categoryId)))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expenses", Long.class)).isZero();
    }

    @Test
    void listAndTotalsExcludeOtherUsers(@Autowired MockMvc mvc, @Autowired JdbcTemplate jdbc,
                                        @Autowired UsersRepository users,
                                        @Autowired org.mromichov.pocket_budget.expenses.ExpensesRepository expenses,
                                        @Autowired org.mromichov.pocket_budget.categories.CategoriesRepository categories)
            throws Exception {
        var other = users.save(new UserEntity("other_" + java.util.UUID.randomUUID().toString().replace("-", ""), "!test", true));
        var category = categories.findById(categoryId).orElseThrow();
        var foreignExpense = expenses.save(new org.mromichov.pocket_budget.expenses.ExpenseEntity(
                777L, other, category, LocalDate.of(2026, 10, 19)));
        mvc.perform(get("/api/expenses").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/expenses").with(user(new BudgetUserDetails(other))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(foreignExpense.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getOwnExpenseById(@Autowired MockMvc mvc, @Autowired ObjectMapper mapper) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");

        mvc.perform(get("/api/expenses/{id}", created.id()).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.amount").value(300))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.category").value("food"))
                .andExpect(jsonPath("$.spentAt").value("2026-10-19"));
    }

    @Test
    void getOtherUsersExpenseByKnownIdReturns404(@Autowired MockMvc mvc,
                                                @Autowired ObjectMapper mapper,
                                                @Autowired UsersRepository users) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");
        var other = users.save(new UserEntity(
                "other_" + java.util.UUID.randomUUID().toString().replace("-", ""),
                "!test", true));

        mvc.perform(get("/api/expenses/{id}", created.id())
                        .with(user(new BudgetUserDetails(other))))
                .andExpect(status().isNotFound());

        // The owner can still read the same expense.
        mvc.perform(get("/api/expenses/{id}", created.id()).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()));
    }

    @Test
    void getMissingExpenseByIdReturns404(@Autowired MockMvc mvc,
                                        @Autowired ObjectMapper mapper,
                                        @Autowired JdbcTemplate jdbc) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");
        jdbc.update("DELETE FROM expenses WHERE id = ?", created.id());

        mvc.perform(get("/api/expenses/{id}", created.id()).with(user(principal)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExpenseByIdWithoutAuthenticationReturns401(@Autowired MockMvc mvc,
                                                      @Autowired ObjectMapper mapper) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");

        mvc.perform(get("/api/expenses/{id}", created.id()).with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteOwnExpenseRemovesItAndKeepsRelatedRows(@Autowired MockMvc mvc,
                                                     @Autowired ObjectMapper mapper,
                                                     @Autowired jakarta.persistence.EntityManager em,
                                                     @Autowired JdbcTemplate jdbc) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");
        mvc.perform(delete("/api/expenses/{id}", created.id()).with(user(principal)).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Force DELETE SQL before checking PostgreSQL directly.
        em.flush();
        em.clear();
        assertEquals(0L, jdbc.queryForObject(
                "SELECT count(*) FROM expenses WHERE id = ?", Long.class, created.id()));
        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM categories WHERE id = ?", Long.class, categoryId));
        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE id = ?", Long.class, principal.getId()));
        mvc.perform(get("/api/expenses/{id}", created.id()).with(user(principal)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOtherUsersExpenseReturns404AndKeepsIt(@Autowired MockMvc mvc,
                                                    @Autowired ObjectMapper mapper,
                                                    @Autowired UsersRepository users,
                                                    @Autowired jakarta.persistence.EntityManager em,
                                                    @Autowired JdbcTemplate jdbc) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");
        var other = users.save(new UserEntity(
                "other_" + java.util.UUID.randomUUID().toString().replace("-", ""),
                "!test", true));
        mvc.perform(delete("/api/expenses/{id}", created.id())
                        .with(user(new BudgetUserDetails(other))).with(csrf()))
                .andExpect(status().isNotFound());

        em.flush();
        em.clear();
        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM expenses WHERE id = ?", Long.class, created.id()));
        mvc.perform(get("/api/expenses/{id}", created.id()).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()));
    }

    @Test
    void deleteMissingExpenseReturns404(@Autowired MockMvc mvc,
                                        @Autowired ObjectMapper mapper,
                                        @Autowired JdbcTemplate jdbc) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");
        jdbc.update("DELETE FROM expenses WHERE id = ?", created.id());
        mvc.perform(delete("/api/expenses/{id}", created.id()).with(user(principal)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutCsrfReturns403AndKeepsExpense(@Autowired MockMvc mvc,
                                                   @Autowired ObjectMapper mapper) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");
        mvc.perform(delete("/api/expenses/{id}", created.id()).with(user(principal)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/expenses/{id}", created.id()).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()));
    }

    @Test
    void deleteWithInvalidCsrfReturns403AndKeepsExpense(@Autowired MockMvc mvc,
                                                       @Autowired ObjectMapper mapper) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");
        mvc.perform(delete("/api/expenses/{id}", created.id())
                        .with(user(principal)).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/expenses/{id}", created.id()).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()));
    }

    @Test
    void deleteWithoutAuthenticationReturns401(@Autowired MockMvc mvc,
                                               @Autowired ObjectMapper mapper) throws Exception {
        Expense created = create(mvc, mapper, "2026-10-19");
        mvc.perform(delete("/api/expenses/{id}", created.id()).with(anonymous()).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/expenses/{id}", created.id()).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()));
    }
}
