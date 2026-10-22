package org.mromichov.pocket_budget.expenses;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigInteger;
import java.time.LocalDate;

public record PostExpenseRequest(
        @NotNull
        @Min(1L)
        @Max(Long.MAX_VALUE)
        BigInteger amount,
        @NotNull
        @NotBlank
        String category,
        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate spentAt
) {
}
