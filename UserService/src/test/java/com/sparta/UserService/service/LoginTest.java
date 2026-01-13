package com.sparta.UserService.service;

import com.sparta.UserService.exception.LoginException;
import com.sparta.UserService.model.UserDetails;
import com.sparta.UserService.repository.RegisterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.UUID;
import java.lang.IllegalArgumentException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginTest {

    @Mock
    private RegisterRepository registerRepository;

    @Mock
    private OTPService otpService;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private AuthService authService;

    private BCryptPasswordEncoder passwordEncoder;
    private UserDetails testUser;
    private String testEmail;
    private String testPassword;
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        testEmail = "test@example.com";
        testPassword = "password123";
        hashedPassword = passwordEncoder.encode(testPassword);

        testUser = new UserDetails();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(testEmail);
        testUser.setPassword(hashedPassword);
        testUser.setFirstname("John");
        testUser.setLastname("Doe");
        testUser.setUsername("johndoe");
    }

    @Test
    void testLogin_Success() {
        // Arrange
        when(registerRepository.findByEmail(testEmail)).thenReturn(testUser);

        // Act
        Map<String, Object> result = authService.login(testEmail, testPassword);

        // Assert
        assertNotNull(result);
        assertEquals("Login successful", result.get("message"));
        assertTrue(result.containsKey("user"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertNotNull(user);
        assertEquals(testUser.getId().toString(), user.get("id"));
        assertEquals(testEmail, user.get("email"));
        assertEquals("John", user.get("firstname"));
        assertEquals("Doe", user.get("lastname"));
        assertEquals("johndoe", user.get("username"));

        verify(registerRepository, times(1)).findByEmail(testEmail);
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        when(registerRepository.findByEmail(testEmail)).thenReturn(null);

        // Act & Assert
        LoginException exception = assertThrows(LoginException.class, () -> {
            authService.login(testEmail, testPassword);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(registerRepository, times(1)).findByEmail(testEmail);
    }

    @Test
    void testLogin_InvalidPassword() {
        // Arrange
        when(registerRepository.findByEmail(testEmail)).thenReturn(testUser);

        // Act & Assert
        LoginException exception = assertThrows(LoginException.class, () -> {
            authService.login(testEmail, "wrongpassword");
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(registerRepository, times(1)).findByEmail(testEmail);
    }

    @Test
    void testLogin_NullEmail() {
        // Arrange
        when(registerRepository.findByEmail(null)).thenReturn(null);

        // Act & Assert
        LoginException exception = assertThrows(LoginException.class, () -> {
            authService.login(null, testPassword);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(registerRepository, times(1)).findByEmail(null);
    }

    @Test
    void testLogin_NullPassword() {
        // Arrange
        when(registerRepository.findByEmail(testEmail)).thenReturn(testUser);

        // Act & Assert
        // BCryptPasswordEncoder throws IllegalArgumentException when password is null
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(testEmail, null);
        });

        assertEquals("rawPassword cannot be null", exception.getMessage());
        verify(registerRepository, times(1)).findByEmail(testEmail);
    }

    @Test
    void testLogin_UserWithNullFields() {
        // Arrange
        UserDetails userWithNulls = new UserDetails();
        userWithNulls.setId(UUID.randomUUID());
        userWithNulls.setEmail(testEmail);
        userWithNulls.setPassword(hashedPassword);
        userWithNulls.setFirstname(null);
        userWithNulls.setLastname(null);
        userWithNulls.setUsername(null);

        when(registerRepository.findByEmail(testEmail)).thenReturn(userWithNulls);

        // Act
        Map<String, Object> result = authService.login(testEmail, testPassword);

        // Assert
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals("", user.get("firstname"));
        assertEquals("", user.get("lastname"));
        assertEquals("", user.get("username"));
    }

    @Test
    void testLogin_EmptyPassword() {
        // Arrange
        when(registerRepository.findByEmail(testEmail)).thenReturn(testUser);

        // Act & Assert
        LoginException exception = assertThrows(LoginException.class, () -> {
            authService.login(testEmail, "");
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(registerRepository, times(1)).findByEmail(testEmail);
    }
}

