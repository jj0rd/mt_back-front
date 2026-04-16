package com.mt.project.Controller;

import com.mt.project.Dto.Login;
import com.mt.project.Model.User;
import com.mt.project.Repository.UserRepository;
import com.mt.project.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    private boolean isPasswordValid(String password) {
        // Co najmniej 12 znaków, 1 mała litera, 1 wielka litera, 1 cyfra, 1 znak specjalny
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-])[A-Za-z\\d@$!%*?&._-]{12,}$";
        return password != null && password.matches(regex);
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody User newUser) {
        // Sprawdzanie, czy użytkownik o podanym e-mailu już istnieje
        User existingUser = userRepository.findByEmail(newUser.getEmail());
        if (existingUser != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("The user with the specified email address already exists.");
        }

        // Weryfikacja poprawności hasła
//        if (!isPasswordValid(newUser.getPassword())) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body("The password must be at least 12 characters long and contain at least one lowercase letter, one uppercase letter, one digit, and one special character.");
//        }

        // Szyfrowanie hasła
        newUser.setPassword(encoder.encode(newUser.getPassword()));

        userRepository.save(newUser);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully.");

        return ResponseEntity.ok(response);
    }
    @PutMapping("/editUser/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User updatedUser) {
        try {
            if (updatedUser.getPassword() != null && !isPasswordValid(updatedUser.getPassword())) {
                return ResponseEntity.badRequest().body(null); // Można zwrócić JSON z błędem
            }

            User updatedRecord = userService.updateUser(id, updatedUser);
            return ResponseEntity.ok(updatedRecord);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/deleteUser/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("User with id " + id + " has been deleted.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // Zwracamy kod 404
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Login loginRequest, HttpServletRequest request) {

        if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            return ResponseEntity.badRequest().body("Email and password must be provided");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");

        } catch (Exception e) {
            e.printStackTrace(); // 🔥 KLUCZOWE
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // Usunięcie sesji
        }
        jakarta.servlet.http.Cookie cookie = new
                jakarta.servlet.http.Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }


    @GetMapping("/users")
    public List<User> getUsers() {
        return null;
    }
}
