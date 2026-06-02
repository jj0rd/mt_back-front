package com.mt.project.ControllerTest;

import com.mt.project.Controller.UserController;
import com.mt.project.Dto.Login;
import com.mt.project.Model.User;
import com.mt.project.Repository.UserRepository;
import com.mt.project.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    // ---------------- REGISTER ----------------

    @Test
    void shouldRegisterUserSuccessfully() {

        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("password1234A!");

        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(null);

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        ResponseEntity<Object> result =
                controller.register(user);

        assertEquals(200, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();

        assertEquals("User registered successfully.", body.get("message"));
    }

    @Test
    void shouldReturnBadRequestWhenUserExists() {

        User user = new User();
        user.setEmail("test@test.com");

        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(new User());

        ResponseEntity<Object> result =
                controller.register(user);

        assertEquals(400, result.getStatusCode().value());
    }

    // ---------------- UPDATE USER ----------------

    @Test
    void shouldUpdateUserSuccessfully() {

        User user = new User();
        user.setPassword("Password1234!");

        User updatedUser = new User();
        updatedUser.setId(1);

        when(userService.updateUser(eq(1), any(User.class)))
                .thenReturn(updatedUser);

        ResponseEntity<User> result =
                controller.updateUser(1, user);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());

        verify(userService).updateUser(eq(1), any(User.class));
    }

    @Test
    void shouldReturnBadRequestWhenPasswordInvalid() {

        User user = new User();
        user.setPassword("weak");

        ResponseEntity<User> result =
                controller.updateUser(1, user);

        assertEquals(400, result.getStatusCode().value());
        assertNull(result.getBody());

        verifyNoInteractions(userService);
    }

    // ---------------- DELETE USER ----------------

    @Test
    void shouldDeleteUserSuccessfully() {

        doNothing().when(userService).deleteUser(1);

        ResponseEntity<String> result =
                controller.deleteUser(1);

        assertEquals(200, result.getStatusCode().value());
        assertTrue(result.getBody().contains("deleted"));

        verify(userService).deleteUser(1);
    }

    // ---------------- LOGIN ----------------

    @Test
    void shouldReturnUnauthorizedOnBadCredentials() {

        Login login = new Login();
        login.setEmail("test@test.com");
        login.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<Object> result =
                controller.login(login, request);

        assertEquals(401, result.getStatusCode().value());
    }

    // ---------------- LOGOUT ----------------

    @Test
    void shouldLogoutSuccessfully() {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        HttpSession session = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(session);

        ResponseEntity<String> result =
                controller.logout(request, response);

        assertEquals(200, result.getStatusCode().value());
        assertEquals("Logged out successfully", result.getBody());

        verify(session).invalidate();
    }
}
