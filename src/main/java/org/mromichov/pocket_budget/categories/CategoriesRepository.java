package org.mromichov.pocket_budget.categories;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriesRepository
        extends JpaRepository<CategoryEntity, Long> {
}