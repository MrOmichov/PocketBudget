package org.mromichov.pocket_budget.expenses;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ExpensesService {
    private final Map<Long, Expense> expenseMap = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0L);
    public List<Expense> expenses() {
        return expenseMap.values().stream().toList();
    }

    public Expense newExpense(PostExpenseRequest request) {
        Expense newExpense = new Expense(nextId.getAndIncrement(), request.amount(), request.category(), request.spentAt());
        expenseMap.put(newExpense.id(), newExpense);
        return newExpense;
    }

}
