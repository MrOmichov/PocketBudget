package org.mromichov.pocket_budget.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity(name = "user")
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 3, max = 50)
    @NotNull
    private String username;

    @Column(name = "password_hash")
    @NotNull
    private String passwordHash;

    private boolean enabled = true;

    protected UserEntity() {
    }

    public UserEntity(String username, String passwordHash, boolean enabled) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
