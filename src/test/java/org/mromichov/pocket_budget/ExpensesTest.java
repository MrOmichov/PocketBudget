package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mromichov.pocket_budget.expenses.ExpensesController;
import org.mromichov.pocket_budget.expenses.ExpensesPageResponse;
import org.mromichov.pocket_budget.expenses.ExpensesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import java.util.List;
import static org.mockito.Mockito.*;

@WebMvcTest(ExpensesController.class)
@AutoConfigureRestTestClient
class ExpensesTest {
    @Autowired private RestTestClient client;
    @MockitoBean private ExpensesService service;

    @Test
    void missingParametersUseDefaults() {
        when(service.expenses(0, 20)).thenReturn(new ExpensesPageResponse(List.of(), 0, 20, 0L, 0));
        client.get().uri("/api/expenses").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().json("""
                        {"items":[],"page":0,"size":20,"totalElements":0,"totalPages":0}
                        """);
        verify(service).expenses(0, 20);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "2,100"})
    void validParametersReachService(int page, int size) {
        when(service.expenses(page, size)).thenReturn(new ExpensesPageResponse(List.of(), page, size, 0L, 0));
        client.get().uri("/api/expenses?page=" + page + "&size=" + size).exchange()
                .expectStatus().isOk();
        verify(service).expenses(page, size);
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,-1", "size,101", "page,abc", "size,abc", "page,2147483648"})
    void invalidParametersDoNotCallService(String parameter, String value) {
        client.get().uri("/api/expenses?" + parameter + "=" + value).exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }
}
