package org.mromichov.pocket_budget.expenses;

import org.mromichov.pocket_budget.categories.CategoriesRepository;
import org.mromichov.pocket_budget.categories.CategoryEntity;
import org.mromichov.pocket_budget.users.UserEntity;
import org.mromichov.pocket_budget.users.UsersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.time.LocalDate;

@Service
public class ExpensesService {
    private final ExpensesRepository expensesRepository;
    private final CategoriesRepository categoriesRepository;
    private final UsersRepository usersRepository;

    public ExpensesService(ExpensesRepository expensesRepository, CategoriesRepository categoriesRepository, UsersRepository usersRepository) {
        this.expensesRepository = expensesRepository;
        this.categoriesRepository = categoriesRepository;
        this.usersRepository = usersRepository;
    }

    @Transactional(readOnly = true)
    public ExpensesPageResponse expenses(long userId, LocalDate from, LocalDate to, Long categoryId, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Начальная дата не может быть после конечной"
            );
        }

        Page<ExpenseEntity> expensesPage = expensesRepository.findByCategoryIdFromTo(
                userId,
                categoryId,
                from,
                to,
                PageRequest.of(
                        page,
                        size,
                        Sort.by("spentAt").descending()
                                .and(Sort.by("id").descending())
                )
        );
        return new ExpensesPageResponse(
                expensesPage.stream().map(ExpensesService::toExpense).toList(),
                expensesPage.getNumber(),
                expensesPage.getSize(),
                expensesPage.getTotalElements(),
                expensesPage.getTotalPages()
        );
    }

    @Transactional
    public void deleteExpenseById(long id, long userId) {
        ExpenseEntity foundEntity = expensesRepository.findByIdAndUser_Id(id, userId).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Этого расхода у вас нет"
                )
        );

        expensesRepository.delete(foundEntity);
    }

    @Transactional(readOnly = true)
    public Expense expenseById(long id, long userId) {
        ExpenseEntity foundEntity = expensesRepository.findByIdAndUser_Id(id, userId).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Этого расхода у вас нет"
                )
        );
        return toExpense(foundEntity);
    }

    @Transactional
    public Expense newExpense(long userId, PostExpenseRequest request) {
        UserEntity user = getUserEntityOrUnauthorized(userId);

        CategoryEntity category = categoriesRepository.findById(request.categoryId())
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Категория не найдена"
                        )
                );

        ExpenseEntity savedEntity = expensesRepository.save(
                new ExpenseEntity(
                        request.amount().longValueExact(),
                        user,
                        category,
                        request.spentAt()
                )
        );
        return toExpense(savedEntity);
    }

    private static Expense toExpense(ExpenseEntity entity) {
        return new Expense(
                entity.getId(),
                BigInteger.valueOf(entity.getAmount()),
                entity.getCategory().getId(),
                entity.getCategory().getName(),
                entity.getSpentAt()
        );
    }

    private UserEntity getUserEntityOrUnauthorized(long userId) {
        return usersRepository.findById(userId).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Пользователь не найден"
                )
        );
    }
}
