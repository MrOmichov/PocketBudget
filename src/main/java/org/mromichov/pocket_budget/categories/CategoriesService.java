package org.mromichov.pocket_budget.categories;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriesService {
    private final CategoriesRepository categoriesRepository;

    public CategoriesService(CategoriesRepository categoriesRepository) {
        this.categoriesRepository = categoriesRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> categories() {
        return categoriesRepository.findAll(Sort.by("id").ascending())
                .stream()
                .map(entity -> new Category(
                                entity.getId(),
                                entity.getName()
                        )
                ).toList();
    }
}
