package org.mromichov.pocket_budget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mromichov.pocket_budget.expenses.*;
import org.mromichov.pocket_budget.users.*;
import org.mromichov.pocket_budget.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpensesController.class)
@Import(SecurityConfig.class)
class ExpensesTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ExpensesService service;
    @MockitoBean private org.mromichov.pocket_budget.categories.CategoriesService categoriesService;
    @MockitoBean private BudgetUserDetailsService userDetailsService;
    private BudgetUserDetails principal;

    @BeforeEach
    void principal() {
        principal = mock(BudgetUserDetails.class);
        when(principal.getId()).thenReturn(42L);
        when(principal.getUsername()).thenReturn("test_user");
        when(principal.getAuthorities()).thenReturn(List.of());
    }

    @Test
    void missingParametersUseDefaults() throws Exception {
        when(service.expenses(42L, 0, 20)).thenReturn(new ExpensesPageResponse(List.of(), 0, 20, 0L, 0));
        mvc.perform(get("/api/expenses").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"items":[],"page":0,"size":20,"totalElements":0,"totalPages":0}
                        """));
        verify(service).expenses(42L, 0, 20);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "2,100"})
    void validParametersReachService(int page, int size) throws Exception {
        when(service.expenses(42L, page, size)).thenReturn(new ExpensesPageResponse(List.of(), page, size, 0L, 0));
        mvc.perform(get("/api/expenses").param("page", Integer.toString(page))
                        .param("size", Integer.toString(size)).with(user(principal)))
                .andExpect(status().isOk());
        verify(service).expenses(42L, page, size);
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,-1", "size,101", "page,abc", "size,abc", "page,2147483648"})
    void invalidParametersDoNotCallService(String parameter, String value) throws Exception {
        mvc.perform(get("/api/expenses").param(parameter, value).with(user(principal)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousGetReturns401WithoutCallingService() throws Exception {
        mvc.perform(get("/api/expenses")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
