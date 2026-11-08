package org.mromichov.pocket_budget.expenses;

import org.mromichov.pocket_budget.expenses.responses.CategoryExpenseSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpensesRepository extends JpaRepository<ExpenseEntity, Long> {
    Page<ExpenseEntity> findByUser_Id(Long userId, Pageable pageable);

    Optional<ExpenseEntity> findByIdAndUser_Id(Long id, Long userId);

    @Query(
            """
                        SELECT e FROM expense e WHERE (e.user.id = :user_id) AND (:category_id IS NULL OR e.category.id = :category_id)
                        AND e.spentAt >= coalesce(:from, e.spentAt)
                        AND e.spentAt <= coalesce(:to, e.spentAt)
                    """
    )
    Page<ExpenseEntity> findByCategoryIdFromTo(
            @Param("user_id")
            Long userId,
            @Param("category_id")
            Long categoryId,
            @Param("from")
            LocalDate from,
            @Param("to")
            LocalDate to,
            Pageable pageable
    );

    @NativeQuery(
            """
                        SELECT
                            categories.id AS "categoryId",
                            categories.name AS "category",
                            COUNT(*) AS "expenseCount",
                            SUM(expenses.amount) AS "totalAmount"
                        FROM expenses
                        INNER JOIN categories ON expenses.category_id = categories.id
                        WHERE (:category_id IS NULL OR expenses.category_id = :category_id) AND expenses.user_id = :user_id
                        AND expenses.spent_at >= coalesce(:from, expenses.spent_at) AND expenses.spent_at <= coalesce(:to, expenses.spent_at)
                        GROUP BY categories.id, categories.name
                        ORDER BY categories.id ASC;
                    """
    )
    List<CategoryExpenseSummaryRow> sumByCategoryIdFromTo(
            @Param("user_id")
            Long userId,
            @Param("category_id")
            Long categoryId,
            @Param("from")
            LocalDate from,
            @Param("to")
            LocalDate to
    );
}
