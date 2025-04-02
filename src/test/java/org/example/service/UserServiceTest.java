package org.example.service;

import org.example.dto.CreateUserRequest;
import org.example.dto.UserResponse;
import org.example.exception.ExpenseSyncException;
import org.example.exception.UserNotFoundException;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateUser_success() {
        CreateUserRequest request = new CreateUserRequest("Krish", "krish@example.com");
        User user = new User("Krish", "krish@example.com");

        when(userRepository.existsByEmail("krish@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("krish@example.com", response.getEmail());
        assertEquals("Krish", response.getName());
    }

    @Test
    void testCreateUser_duplicateEmail_throwsException() {
        CreateUserRequest request = new CreateUserRequest("Krish", "krish@example.com");
        when(userRepository.existsByEmail("krish@example.com")).thenReturn(true);

        assertThrows(ExpenseSyncException.class, () -> userService.createUser(request));
    }

    @Test
    void testGetUserByEmail_found() {
        User user = new User("Krish", "krish@example.com");
        when(userRepository.findByEmail("krish@example.com")).thenReturn(Optional.of(user));

        User result = userService.getUserByEmail("krish@example.com");

        assertNotNull(result);
        assertEquals("krish@example.com", result.getEmail());
    }

    @Test
    void testGetUserByEmail_notFound_throwsException() {
        when(userRepository.findByEmail("xyz@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByEmail("xyz@example.com"));
    }

    @Test
    void testGetAllUsers_withEmail_found() {
        User user = new User("Krish", "krish@example.com");
        when(userRepository.findByEmail("krish@example.com")).thenReturn(Optional.of(user));

        List<UserResponse> users = userService.getAllUsers("krish@example.com");

        assertEquals(1, users.size());
        assertEquals("krish@example.com", users.get(0).getEmail());
    }

    @Test
    void testGetAllUsers_withNoEmail_returnsAll() {
        User user1 = new User("Krish", "krish@example.com");
        User user2 = new User("Janhvi", "janhvi@example.com");
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> users = userService.getAllUsers(null);

        assertEquals(2, users.size());
    }

    @Test
    void testGetAllUsersByEmail_returnsSet() {
        User u1 = new User("A", "a@example.com");
        User u2 = new User("B", "b@example.com");

        when(userRepository.findAllByEmailIn(any())).thenReturn(List.of(u1, u2));

        Set<User> users = userService.getAllUsersByEmail(Set.of("a@example.com", "b@example.com"));
        assertEquals(2, users.size());
    }
}
