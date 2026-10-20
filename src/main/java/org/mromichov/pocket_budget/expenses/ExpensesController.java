package org.mromichov.pocket_budget.expenses;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ExpensesController {
    private final ExpensesService expensesService;

    public ExpensesController(ExpensesService expensesService) {
        this.expensesService = expensesService;
    }

    @GetMapping("/api/expenses")
    public List<Expense> expenses() {
        return expensesService.expenses();
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
        return ResponseEntity.created(URI.create("/api/expenses/"+expense.id())).body(expense);
    }
}
