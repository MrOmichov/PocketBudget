package org.mromichov.pocket_budget.expenses.responses;

import java.math.BigDecimal;

public interface CategoryExpenseSummaryRow {
    Long getCategoryId();

    String getCategory();

    Long getExpenseCount();

    BigDecimal getTotalAmount();
}
