package com.sparta.UserService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.UserService.exception.SignupException;
import com.sparta.UserService.model.SignupData;
import com.sparta.UserService.model.UserDetails;
import com.sparta.UserService.repository.RegisterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifySignupOTPTest {

    @Mock
    private RegisterRepository registerRepository;

    @Mock
    private OTPService otpService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuthService authService;

    private String testEmail;
    private String correctOTP;
    private String wrongOTP;
    private SignupData signupData;
    private UserDetails savedUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testEmail = "newuser@example.com";
        correctOTP = "123456";
        wrongOTP = "000000";
        testUserId = UUID.randomUUID();

        signupData = new SignupData();
        signupData.setEmail(testEmail);
        signupData.setPassword("$2a$10$hashedpassword");
        signupData.setFirstname("Jane");
        signupData.setLastname("Smith");
        signupData.setUsername("janesmith");
        signupData.setOtp(correctOTP);
        signupData.setCreatedAt(System.currentTimeMillis());

        savedUser = new UserDetails();
        savedUser.setId(testUserId);
        savedUser.setEmail(testEmail);
        savedUser.setPassword(signupData.getPassword());
        savedUser.setFirstname(signupData.getFirstname());
        savedUser.setLastname(signupData.getLastname());
        savedUser.setUsername(signupData.getUsername());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testVerifySignupOTP_Success() {
        // Arrange
        when(valueOperations.get("signup:otp:" + testEmail)).thenReturn(signupData);
        when(objectMapper.convertValue(signupData, SignupData.class)).thenReturn(signupData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);
        when(registerRepository.existsByEmail(testEmail)).thenReturn(false);
        when(registerRepository.save(any(UserDetails.class))).thenReturn(savedUser);

        // Act
        Map<String, Object> result = authService.verifySignupOTP(testEmail, correctOTP);

        // Assert
        assertNotNull(result);
        assertEquals("Signup successful", result.get("message"));
        assertTrue(result.containsKey("user"));

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals(testUserId.toString(), user.get("id"));
        assertEquals(testEmail, user.get("email"));
        assertEquals("Jane", user.get("firstname"));
        assertEquals("Smith", user.get("lastname"));
        assertEquals("janesmith", user.get("username"));

        verify(valueOperations, times(1)).get("signup:otp:" + testEmail);
        verify(otpService, times(1)).verifyOTP(correctOTP, correctOTP);
        verify(registerRepository, times(1)).existsByEmail(testEmail);
        verify(registerRepository, times(1)).save(any(UserDetails.class));
        verify(redisTemplate, times(1)).delete("signup:otp:" + testEmail);
    }

    @Test
    void testVerifySignupOTP_OTPExpired() {
        // Arrange
        when(valueOperations.get("signup:otp:" + testEmail)).thenReturn(null);

        // Act & Assert
        SignupException exception = assertThrows(SignupException.class, () -> {
            authService.verifySignupOTP(testEmail, correctOTP);
        });

        assertEquals("OTP expired or invalid. Please request a new OTP.", exception.getMessage());
        verify(valueOperations, times(1)).get("signup:otp:" + testEmail);
        verify(otpService, never()).verifyOTP(anyString(), anyString());
        verify(registerRepository, never()).save(any(UserDetails.class));
    }

    @Test
    void testVerifySignupOTP_InvalidOTP() {
        // Arrange
        when(valueOperations.get("signup:otp:" + testEmail)).thenReturn(signupData);
        when(objectMapper.convertValue(signupData, SignupData.class)).thenReturn(signupData);
        when(otpService.verifyOTP(wrongOTP, correctOTP)).thenReturn(false);

        // Act & Assert
        SignupException exception = assertThrows(SignupException.class, () -> {
            authService.verifySignupOTP(testEmail, wrongOTP);
        });

        assertEquals("Invalid OTP. Please try again.", exception.getMessage());
        verify(valueOperations, times(1)).get("signup:otp:" + testEmail);
        verify(otpService, times(1)).verifyOTP(wrongOTP, correctOTP);
        verify(registerRepository, never()).save(any(UserDetails.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testVerifySignupOTP_EmailAlreadyExists() {
        // Arrange
        when(valueOperations.get("signup:otp:" + testEmail)).thenReturn(signupData);
        when(objectMapper.convertValue(signupData, SignupData.class)).thenReturn(signupData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);
        when(registerRepository.existsByEmail(testEmail)).thenReturn(true);

        // Act & Assert
        SignupException exception = assertThrows(SignupException.class, () -> {
            authService.verifySignupOTP(testEmail, correctOTP);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(valueOperations, times(1)).get("signup:otp:" + testEmail);
        verify(otpService, times(1)).verifyOTP(correctOTP, correctOTP);
        verify(registerRepository, times(1)).existsByEmail(testEmail);
        verify(registerRepository, never()).save(any(UserDetails.class));
        verify(redisTemplate, times(1)).delete("signup:otp:" + testEmail);
    }

    @Test
    void testVerifySignupOTP_NullEmail() {
        // Arrange
        when(valueOperations.get("signup:otp:null")).thenReturn(null);

        // Act & Assert
        SignupException exception = assertThrows(SignupException.class, () -> {
            authService.verifySignupOTP(null, correctOTP);
        });

        assertEquals("OTP expired or invalid. Please request a new OTP.", exception.getMessage());
    }

    @Test
    void testVerifySignupOTP_NullOTP() {
        // Arrange
        when(valueOperations.get("signup:otp:" + testEmail)).thenReturn(signupData);
        when(objectMapper.convertValue(signupData, SignupData.class)).thenReturn(signupData);
        when(otpService.verifyOTP(null, correctOTP)).thenReturn(false);

        // Act & Assert
        SignupException exception = assertThrows(SignupException.class, () -> {
            authService.verifySignupOTP(testEmail, null);
        });

        assertEquals("Invalid OTP. Please try again.", exception.getMessage());
    }

    @Test
    void testVerifySignupOTP_UserWithNullFields() {
        // Arrange
        signupData.setFirstname(null);
        signupData.setLastname(null);
        signupData.setUsername(null);
        savedUser.setFirstname(null);
        savedUser.setLastname(null);
        savedUser.setUsername(null);

        when(valueOperations.get("signup:otp:" + testEmail)).thenReturn(signupData);
        when(objectMapper.convertValue(signupData, SignupData.class)).thenReturn(signupData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);
        when(registerRepository.existsByEmail(testEmail)).thenReturn(false);
        when(registerRepository.save(any(UserDetails.class))).thenReturn(savedUser);

        // Act
        Map<String, Object> result = authService.verifySignupOTP(testEmail, correctOTP);

        // Assert
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals("", user.get("firstname"));
        assertEquals("", user.get("lastname"));
        assertEquals("", user.get("username"));
    }

    @Test
    void testVerifySignupOTP_FailedToGenerateUserId() {
        // Arrange
        UserDetails userWithoutId = new UserDetails();
        userWithoutId.setId(null);
        userWithoutId.setEmail(testEmail);

        when(valueOperations.get("signup:otp:" + testEmail)).thenReturn(signupData);
        when(objectMapper.convertValue(signupData, SignupData.class)).thenReturn(signupData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);
        when(registerRepository.existsByEmail(testEmail)).thenReturn(false);
        when(registerRepository.save(any(UserDetails.class))).thenReturn(userWithoutId);

        // Act & Assert
        SignupException exception = assertThrows(SignupException.class, () -> {
            authService.verifySignupOTP(testEmail, correctOTP);
        });

        assertEquals("Failed to generate user ID. Please try again.", exception.getMessage());
        verify(registerRepository, times(1)).save(any(UserDetails.class));
    }

    @Test
    void testVerifySignupOTP_EmptyOTP() {
        // Arrange
        when(valueOperations.get("signup:otp:" + testEmail)).thenReturn(signupData);
        when(objectMapper.convertValue(signupData, SignupData.class)).thenReturn(signupData);
        when(otpService.verifyOTP("", correctOTP)).thenReturn(false);

        // Act & Assert
        SignupException exception = assertThrows(SignupException.class, () -> {
            authService.verifySignupOTP(testEmail, "");
        });

        assertEquals("Invalid OTP. Please try again.", exception.getMessage());
    }
}

