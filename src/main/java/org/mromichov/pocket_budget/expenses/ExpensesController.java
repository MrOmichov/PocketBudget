package org.mromichov.pocket_budget.expenses;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ExpensesController {
    private final ExpensesService expensesService;

    public ExpensesController(ExpensesService expensesService) {
        this.expensesService = expensesService;
    }

    @GetMapping("/api/expenses")
    public ResponseEntity<?> expenses(
            @RequestParam(defaultValue = "0")
            @Min(0L)
            int page,
            @RequestParam(defaultValue = "20")
            @Max(100L)
            @Min(1L)
            int size
    ) {
        return ResponseEntity.ok(expensesService.expenses(page, size));
    }

    @PostMapping("/api/expenses")
    public ResponseEntity<?> newExpense(@Valid @RequestBody PostExpenseRequest request, Errors errors) {
        if (errors.hasErrors()) {
            Map<String, String> bodyErrors = errors.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Неверное значение",
                            (existing, replacement) -> existing
                    ));
            return ResponseEntity.badRequest().body(bodyErrors);
        }
        Expense expense = expensesService.newExpense(request);
        return ResponseEntity.created(URI.create("/api/expenses/" + expense.id())).body(expense);
    }
}
