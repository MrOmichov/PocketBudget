package org.mromichov.pocket_budget.users;

import org.springframework.security.core.userdetails.User;

import java.util.List;

public class BudgetUserDetails extends User {
    private final long id;

    public BudgetUserDetails(UserEntity user) {
        super(
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                true,
                true,
                true,
                List.of()
        );
        this.id = user.getId();
    }

    public long getId() {
        return id;
    }
}
