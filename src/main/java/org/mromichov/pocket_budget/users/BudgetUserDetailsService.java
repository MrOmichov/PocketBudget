package org.mromichov.pocket_budget.users;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BudgetUserDetailsService implements UserDetailsService {
    private final UsersRepository usersRepository;

    public BudgetUserDetailsService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserEntity> users = usersRepository.findByUsername(username);
        if (users.isPresent()) {
            return new BudgetUserDetails(users.get());
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }
}
