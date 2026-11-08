package org.mromichov.pocket_budget.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9_]{3,50}$")
        String username,
        @NotBlank
        @Size(min = 8, max = 64)
        String password
) {

}
