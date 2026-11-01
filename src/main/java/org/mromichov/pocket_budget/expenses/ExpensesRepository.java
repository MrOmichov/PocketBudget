package org.mromichov.pocket_budget.expenses;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpensesRepository extends JpaRepository<ExpenseEntity, Long> {
    Page<ExpenseEntity> findByUser_Id(Long userId, Pageable pageable);

    Optional<ExpenseEntity> findByIdAndUser_Id(Long id, Long userId);
}
