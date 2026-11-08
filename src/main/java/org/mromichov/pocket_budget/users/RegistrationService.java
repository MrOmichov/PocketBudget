package org.mromichov.pocket_budget.users;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisteredUserResponse registerUser(RegisterUserRequest request) {
        UserEntity userToSave = new UserEntity(request.username(), passwordEncoder.encode(request.password()), true);
        UserEntity savedUser = usersRepository.saveAndFlush(userToSave);

        return new RegisteredUserResponse(
                savedUser.getId(),
                savedUser.getUsername()
        );
    }
}
