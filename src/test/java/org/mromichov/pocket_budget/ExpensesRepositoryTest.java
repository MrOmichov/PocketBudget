package org.mromichov.pocket_budget;

import jakarta.persistence.EntityManager;
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
    @Test
    void savesAndReadsExpense(@Autowired ExpensesRepository repository, @Autowired EntityManager entityManager) {
        long amount = 300L;
        String category = "food";
        LocalDate spentAt = LocalDate.of(2026, 10, 19);

        ExpenseEntity expenseEntity = new ExpenseEntity(amount, category, spentAt);
        assertNull(expenseEntity.getId());
        ExpenseEntity returnedExpenseEntity = repository.save(expenseEntity);

        Long expenseId = returnedExpenseEntity.getId();
        assertNotNull(expenseId);
        assertEquals(amount, returnedExpenseEntity.getAmount());
        assertEquals(category, returnedExpenseEntity.getCategory());
        assertEquals(spentAt, returnedExpenseEntity.getSpentAt());

        repository.flush();
        entityManager.clear();

        ExpenseEntity foundByIdExpenseEntity = repository.findById(expenseId).orElseThrow();
        assertNotNull(foundByIdExpenseEntity);

        assertEquals(expenseId, foundByIdExpenseEntity.getId());
        assertNotSame(returnedExpenseEntity, foundByIdExpenseEntity);
        assertEquals(amount, foundByIdExpenseEntity.getAmount());
        assertEquals(category, foundByIdExpenseEntity.getCategory());
        assertEquals(spentAt, foundByIdExpenseEntity.getSpentAt());
    }

}
