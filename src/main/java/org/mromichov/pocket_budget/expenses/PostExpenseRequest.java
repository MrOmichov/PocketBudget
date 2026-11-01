package org.mromichov.pocket_budget.expenses;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigInteger;
import java.time.LocalDate;

public record PostExpenseRequest(
        @NotNull
        @Min(1L)
        @Max(Long.MAX_VALUE)
        BigInteger amount,
        @NotNull
        @Positive
        Long categoryId,
        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate spentAt
) {
}
