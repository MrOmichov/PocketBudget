package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.mromichov.pocket_budget.expenses.Expense;
import org.mromichov.pocket_budget.expenses.ExpensesController;
import org.mromichov.pocket_budget.expenses.ExpensesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;


@WebMvcTest(ExpensesController.class)
@AutoConfigureRestTestClient
public class ExpensesTest {
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private ExpensesService expensesService;

    @Test
    void expenseShouldReturnEmptyArray() {
        when(expensesService.expenses()).thenReturn(new ArrayList<>());
        restTestClient.get()
                .uri("/api/expenses")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("[]");
    }

    @Test
    void expenseMockShouldReturnArrayWithOneExpense() {
        List<Expense> oneExpense = new ArrayList<>();
        oneExpense.add(new Expense(0, new BigInteger("300"), "food", LocalDate.of(2026, 10, 19)));
        when(expensesService.expenses()).thenReturn(oneExpense);
        restTestClient.get()
                .uri("/api/expenses")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$").value((List<?> expenses) -> assertThat(expenses, hasSize(1)))
                .jsonPath("$[0].id").isEqualTo(0)
                .jsonPath("$[0].amount").isEqualTo(300)
                .jsonPath("$[0].category").isEqualTo("food")
                .jsonPath("$[0].spentAt").isEqualTo("2026-10-19");

    }
}
