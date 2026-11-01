package org.mromichov.pocket_budget;

import jakarta.persistence.EntityManager;
import org.mromichov.pocket_budget.users.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import org.junit.jupiter.api.Test;
import org.mromichov.pocket_budget.expenses.ExpenseEntity;
import org.mromichov.pocket_budget.expenses.ExpensesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ExpensesRepositoryTest {
    @org.junit.jupiter.api.BeforeEach
    void checkDatabase(@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc) {
        assertEquals("pocket_budget_test", jdbc.queryForObject("SELECT current_database()", String.class));
    }

    @Test
    void savesAndReadsExpense(@Autowired ExpensesRepository repository, @Autowired EntityManager entityManager,
                            @Autowired org.mromichov.pocket_budget.categories.CategoriesRepository categories,
                            @Autowired UsersRepository users) {
        var owner = users.save(new UserEntity("test_" + java.util.UUID.randomUUID().toString().replace("-", ""), "!test", true));
        long amount = 300L;
        var category = categories.save(new org.mromichov.pocket_budget.categories.CategoryEntity(
                "repository-" + java.util.UUID.randomUUID()));
        LocalDate spentAt = LocalDate.of(2026, 10, 19);

        ExpenseEntity expenseEntity = new ExpenseEntity(amount, owner, category, spentAt);
        assertNull(expenseEntity.getId());
        ExpenseEntity returnedExpenseEntity = repository.save(expenseEntity);

        Long expenseId = returnedExpenseEntity.getId();
        assertNotNull(expenseId);
        assertEquals(amount, returnedExpenseEntity.getAmount());
        assertEquals(category.getId(), returnedExpenseEntity.getCategory().getId());
        assertEquals(spentAt, returnedExpenseEntity.getSpentAt());

        repository.flush();
        entityManager.clear();

        ExpenseEntity foundByIdExpenseEntity = repository.findById(expenseId).orElseThrow();
        assertNotNull(foundByIdExpenseEntity);

        assertEquals(expenseId, foundByIdExpenseEntity.getId());
        assertNotSame(returnedExpenseEntity, foundByIdExpenseEntity);
        assertEquals(owner.getId(), foundByIdExpenseEntity.getUser().getId());
        assertEquals(amount, foundByIdExpenseEntity.getAmount());
        assertEquals(category.getId(), foundByIdExpenseEntity.getCategory().getId());
        assertEquals(category.getName(), foundByIdExpenseEntity.getCategory().getName());
        assertEquals(spentAt, foundByIdExpenseEntity.getSpentAt());
    }

}
