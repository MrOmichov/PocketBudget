package org.mromichov.pocket_budget.expenses;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

@Repository
public class ExpensesRepository {
    private final JdbcTemplate template;

    private final RowMapper<Expense> expenseMapper = (ResultSet rs, int rowNumber) -> new Expense(
            rs.getLong("id"),
            BigInteger.valueOf(rs.getLong("amount")),
            rs.getString("category"),
            rs.getObject("spent_at", LocalDate.class));

    public ExpensesRepository(JdbcTemplate template) {
        this.template = template;
    }

    public List<Expense> getAllExpenses() {
        return template.query(
                """
                             select id, amount, category, spent_at from expenses;
                        """,
                expenseMapper
        );
    }

    public Expense newExpense(PostExpenseRequest request) {
        return template.queryForObject(
                """
                             insert into expenses (amount, category, spent_at)
                             values (?, ?, ?) RETURNING id, amount, category, spent_at;
                        """,
                expenseMapper,
                request.amount().longValueExact(),
                request.category(),
                request.spentAt());

    }
}
