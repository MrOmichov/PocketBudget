package org.mromichov.pocket_budget.expenses;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpensesService {
    private final ExpensesRepository expensesRepository;

    public ExpensesService(ExpensesRepository expensesRepository) {
        this.expensesRepository = expensesRepository;
    }

    public List<Expense> expenses() {
        return expensesRepository.getAllExpenses();
    }

    public Expense newExpense(PostExpenseRequest request) {
        return expensesRepository.newExpense(request);
    }

}
