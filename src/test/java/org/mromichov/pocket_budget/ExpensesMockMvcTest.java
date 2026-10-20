package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.mromichov.pocket_budget.expenses.Expense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(createdExpense.id()))
                .andExpect(jsonPath("$[0].amount").value(300))
                .andExpect(jsonPath("$[0].category").value("food"))
                .andExpect(jsonPath("$[0].spentAt").value("2026-10-19"));
    }

    @Test
    void postTwoExpenses(
            @Autowired MockMvc mvc,
            @Autowired ObjectMapper objectMapper
    ) throws Exception {
        String request = """
            {
                "amount": 300,
                "category": "food",
                "spentAt": "2026-10-19"
            }
            """;

        Expense[] created = new Expense[2];

        for (int i = 0; i < created.length; i++) {
            MvcResult result = mvc.perform(post("/api/expenses")
                            .contentType("application/json")
                            .content(request))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andReturn();

            created[i] = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    Expense.class
            );
        }

        assertThat(created[0].id()).isNotEqualTo(created[1].id());

        MvcResult getResult = mvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn();

        Expense[] actual = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                Expense[].class
        );

        assertThat(actual).containsExactlyInAnyOrder(created);
    }

    @Test
    void postWrongAmountExpenses(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/api/expenses").contentType("application/json").content(
                        """
                        {
                            "amount": -1,
                            "category": "food",
                            "spentAt": "2026-10-19"
                        }
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/expenses").contentType("application/json").content(
                        """
                        {
                            "amount": 0,
                            "category": "food",
                            "spentAt": "2026-10-19"
                        }
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/expenses").contentType("application/json").content(
                        """
                        {
                            "category": "food",
                            "spentAt": "2026-10-19"
                        }
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void postWrongCategoryAndSpentAtExpenses(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/api/expenses").contentType("application/json").content(
                        """
                        {
                            "amount": 300,
                            "category": " ",
                            "spentAt": "2026-10-19"
                        }
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/expenses").contentType("application/json").content(
                        """
                        {
                            "amount": 300,
                            "category": "",
                            "spentAt": "2026-10-19"
                        }
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/expenses").contentType("application/json").content(
                        """
                        {
                            "amount": 300,
                            "spentAt": "2026-10-19"
                        }
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/expenses").contentType("application/json").content(
                        """
                        {
                            "amount": 300,
                            "category": "food"
                        }
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void postNullFieldsDoesNotSaveExpense(@Autowired MockMvc mvc)
            throws Exception {
        String[] requests = {
                """
            {
                "amount": null,
                "category": "food",
                "spentAt": "2026-10-19"
            }
            """,
                """
            {
                "amount": 300,
                "category": null,
                "spentAt": "2026-10-19"
            }
            """,
                """
            {
                "amount": 300,
                "category": "food",
                "spentAt": null
            }
            """
        };

        for (String request : requests) {
            mvc.perform(post("/api/expenses")
                            .contentType("application/json")
                            .content(request))
                    .andExpect(status().isBadRequest());

            mvc.perform(get("/api/expenses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
