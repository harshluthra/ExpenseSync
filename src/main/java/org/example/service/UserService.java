package org.example.service;

import org.apache.commons.lang3.StringUtils;
import org.example.dto.CreateUserRequest;
import org.example.dto.UserResponse;
import org.example.exception.ExpenseSyncException;
import org.example.exception.UserNotFoundException;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ExpenseSyncException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        User savedUser = userRepository.save(user);
        return UserResponse.builder()
                .uuid(savedUser.getUuid()).name(savedUser.getName()).email(savedUser.getEmail())
                .build();
    }

    public List<UserResponse> getAllUsers(String email) {
        List<User> users;
        if (StringUtils.isNotEmpty(email)) {
            users = List.of(userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User with email '" + email + "' not found.")));
        } else {
            users = userRepository.findAll();
        }

        return users.stream().map(user ->
                        UserResponse.builder().uuid(user.getUuid()).name(user.getName()).email(user.getEmail()).build())
                .toList();
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email '" + email + "' not found."));
    }

    public Set<User> getAllUsersByEmail(Iterable<String> email) {
        return new HashSet<>(userRepository.findAllByEmailIn(email));
    }
}
