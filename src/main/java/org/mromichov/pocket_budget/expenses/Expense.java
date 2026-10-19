package org.mromichov.pocket_budget.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Expense(
        long id,
        BigDecimal amount,
        String category,
        LocalDate spentAt
) {}
