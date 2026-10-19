package org.mromichov.pocket_budget.expenses;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExpensesService {
    public List<Expense> expenses() {
        return new ArrayList<>();
    }
}
