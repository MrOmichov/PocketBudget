package org.mromichov.pocket_budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.mromichov.pocket_budget.categories.CategoriesService;
import org.mromichov.pocket_budget.categories.Category;
import org.mromichov.pocket_budget.expenses.Expense;
import org.mromichov.pocket_budget.expenses.ExpensesService;
import org.mromichov.pocket_budget.expenses.PostExpenseRequest;
import org.mromichov.pocket_budget.expenses.responses.ExpensesSummaryResponse;
import org.mromichov.pocket_budget.users.BudgetUserDetails;
import org.mromichov.pocket_budget.users.RegisterUserRequest;
import org.mromichov.pocket_budget.users.RegisteredUserResponse;
import org.mromichov.pocket_budget.users.RegistrationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class BudgetController {
    private final ExpensesService expensesService;
    private final CategoriesService categoriesService;
    private final RegistrationService registrationService;

    public BudgetController(ExpensesService expensesService, CategoriesService categoriesService, RegistrationService registrationService) {
        this.expensesService = expensesService;
        this.categoriesService = categoriesService;
        this.registrationService = registrationService;
    }

    @GetMapping("/api/categories")
    public List<Category> categories() {
        return categoriesService.categories();
    }

    @GetMapping("/api/expenses")
    public ResponseEntity<?> expenses(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false)
            @Min(1L)
            Long categoryId,
            @RequestParam(defaultValue = "0")
            @Min(0L)
            int page,
            @RequestParam(defaultValue = "20")
            @Max(100L)
            @Min(1L)
            int size,
            @AuthenticationPrincipal BudgetUserDetails currentUser
    ) {
        return ResponseEntity.ok(expensesService.expenses(
                        currentUser.getId(),
                        from,
                        to,
                        categoryId,
                        page,
                        size
                )
        );
    }

    @GetMapping("/api/expenses/summary")
    public ExpensesSummaryResponse sumExpenses(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false)
            @Min(1L)
            Long categoryId,
            @AuthenticationPrincipal BudgetUserDetails currentUser
    ) {
        return expensesService.sumExpenses(currentUser.getId(), from, to, categoryId);
    }

    @GetMapping("/api/expenses/{id}")
    public Expense expenseById(
            @PathVariable
            Long id,

            @AuthenticationPrincipal
            BudgetUserDetails currentUser
    ) {
        return expensesService.expenseById(id, currentUser.getId());
    }


    @DeleteMapping("/api/expenses/{id}")
    public ResponseEntity<Void> deleteExpenseById(
            @PathVariable
            Long id,
            @AuthenticationPrincipal
            BudgetUserDetails currentUser
    ) {
        expensesService.deleteExpenseById(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/expenses")
    public ResponseEntity<?> newExpense(
            @Valid @RequestBody PostExpenseRequest request,
            Errors errors,
            @AuthenticationPrincipal BudgetUserDetails currentUser
    ) {
        if (errors.hasErrors()) {
            Map<String, String> bodyErrors = errors.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Неверное значение",
                            (existing, replacement) -> existing
                    ));
            return ResponseEntity.badRequest().body(bodyErrors);
        }
        Expense expense = expensesService.newExpense(currentUser.getId(), request);
        return ResponseEntity.created(URI.create("/api/expenses/" + expense.id())).body(expense);
    }


    @PostMapping("/api/auth/register")
    public ResponseEntity<RegisteredUserResponse> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        RegisteredUserResponse response = registrationService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
