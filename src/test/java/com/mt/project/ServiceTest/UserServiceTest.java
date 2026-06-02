package com.mt.project.ServiceTest;

import com.mt.project.Model.User;
import com.mt.project.Repository.UserRepository;
import com.mt.project.Service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldUpdateUserSuccessfully() {

        Integer userId = 1;

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("old@mail.com");
        existingUser.setName("Old");
        existingUser.setSurname("User");
        existingUser.setPassword("oldpass");

        User updatedUser = new User();
        updatedUser.setEmail("new@mail.com");
        updatedUser.setName("New");
        updatedUser.setSurname("Name");
        updatedUser.setPassword("newpass123");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(userId, updatedUser);

        assertNotNull(result);
        assertEquals("new@mail.com", result.getEmail());
        assertEquals("New", result.getName());
        assertEquals("Name", result.getSurname());

        // password powinno być zakodowane (nie równe plain text)
        assertNotEquals("newpass123", result.getPassword());

        verify(userRepository).findById(userId);
        verify(userRepository).save(existingUser);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundOnUpdate() {

        when(userRepository.findById(1))
                .thenReturn(Optional.empty());

        User updatedUser = new User();
        updatedUser.setPassword("test");

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(1, updatedUser)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldDeleteUserSuccessfully() {

        User user = new User();
        user.setId(1);

        when(userRepository.findById(1))
                .thenReturn(Optional.of(user));

        doNothing().when(userRepository).delete(user);

        userService.deleteUser(1);

        verify(userRepository).delete(user);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingUser() {

        when(userRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.deleteUser(1)
        );

        verify(userRepository, never()).delete(any());
    }

    @Test
    void shouldFindUserByEmail() {

        User user = new User();
        user.setEmail("test@mail.com");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(user);

        User result = userService.findByEmail("test@mail.com");

        assertNotNull(result);
        assertEquals("test@mail.com", result.getEmail());
    }

    @Test
    void shouldReturnUserById() {

        User user = new User();
        user.setId(1);

        when(userRepository.findById(1))
                .thenReturn(Optional.of(user));

        User result = userService.findUserById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void shouldReturnNullWhenUserByIdNotFound() {

        when(userRepository.findById(1))
                .thenReturn(Optional.empty());

        User result = userService.findUserById(1);

        assertNull(result);
    }
}
