package org.mromichov.pocket_budget.expenses;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigInteger;
import java.time.LocalDate;

public record PostExpenseRequest(
        @NotNull
        @Min(1L)
        BigInteger amount,
        @NotNull
        @NotBlank
        String category,
        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate spentAt
) {
}
