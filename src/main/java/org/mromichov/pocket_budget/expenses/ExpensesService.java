package org.mromichov.pocket_budget.expenses;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Service
public class ExpensesService {
    private final ExpensesRepository expensesRepository;

    public ExpensesService(ExpensesRepository expensesRepository) {
        this.expensesRepository = expensesRepository;
    }

    @Transactional(readOnly = true)
    public ExpensesPageResponse expenses(int page, int size) {
        Page<ExpenseEntity> expensesPage = expensesRepository.findAll(
                PageRequest.of(
                        page,
                        size,
                        Sort.by("spentAt").descending()
                                .and(Sort.by("id").descending())
                )
        );
        return new ExpensesPageResponse(
                expensesPage.stream().map(ExpensesService::toExpense).toList(),
                expensesPage.getNumber(),
                expensesPage.getSize(),
                expensesPage.getTotalElements(),
                expensesPage.getTotalPages()
        );
    }

    @Transactional
    public Expense newExpense(PostExpenseRequest request) {
        ExpenseEntity savedEntity = expensesRepository.save(
                new ExpenseEntity(
                        request.amount().longValueExact(),
                        request.category(),
                        request.spentAt()
                )
        );
        return toExpense(savedEntity);
    }

    private static Expense toExpense(ExpenseEntity entity) {
        return new Expense(
                entity.getId(),
                BigInteger.valueOf(entity.getAmount()),
                entity.getCategory(),
                entity.getSpentAt()
        );
    }
}
