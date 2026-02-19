package com.sparta.UserService.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.UserService.exception.ForgotException;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyForgotPasswordOTPTest {

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

    @Mock
    private com.sparta.UserService.util.JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private String testEmail;
    private String correctOTP;
    private String wrongOTP;
    private UUID testUserId;
    private UserDetails testUser;
    private Map<String, String> forgotPasswordData;

    @BeforeEach
    void setUp() {
        testEmail = "user@example.com";
        correctOTP = "123456";
        wrongOTP = "000000";
        testUserId = UUID.randomUUID();

        testUser = new UserDetails();
        testUser.setId(testUserId);
        testUser.setEmail(testEmail);
        testUser.setFirstname("John");
        testUser.setLastname("Doe");
        testUser.setUsername("johndoe");

        forgotPasswordData = new HashMap<>();
        forgotPasswordData.put("userId", testUserId.toString());
        forgotPasswordData.put("email", testEmail);
        forgotPasswordData.put("otp", correctOTP);
        forgotPasswordData.put("createdAt", String.valueOf(System.currentTimeMillis()));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testVerifyForgotPasswordOTP_Success() {
        // Arrange
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);
        when(registerRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(UUID.class), anyString())).thenReturn("mock-access-token");
        when(jwtUtil.generateRefreshToken(any(UUID.class), anyString())).thenReturn("mock-refresh-token");

        // Act
        Map<String, Object> result = authService.verifyForgotPasswordOTP(testEmail, correctOTP);

        // Assert
        assertNotNull(result);
        assertEquals("OTP verified successfully", result.get("message"));
        assertTrue(result.containsKey("user"));

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals(testUserId.toString(), user.get("id"));
        assertEquals(testEmail, user.get("email"));
        assertEquals("John", user.get("firstname"));
        assertEquals("Doe", user.get("lastname"));
        assertEquals("johndoe", user.get("username"));

        verify(valueOperations, times(1)).get("forgot:otp:" + testEmail);
        verify(otpService, times(1)).verifyOTP(correctOTP, correctOTP);
        verify(registerRepository, times(1)).findById(testUserId);
        verify(redisTemplate, times(1)).delete("forgot:otp:" + testEmail);
    }

    @Test
    void testVerifyForgotPasswordOTP_OTPExpired() {
        // Arrange
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(null);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(testEmail, correctOTP);
        });

        assertEquals("OTP expired or invalid. Please request a new OTP.", exception.getMessage());
        verify(valueOperations, times(1)).get("forgot:otp:" + testEmail);
        verify(otpService, never()).verifyOTP(anyString(), anyString());
        verify(registerRepository, never()).findById(any(UUID.class));
    }

    @Test
    void testVerifyForgotPasswordOTP_InvalidOTP() {
        // Arrange
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP(wrongOTP, correctOTP)).thenReturn(false);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(testEmail, wrongOTP);
        });

        assertEquals("Invalid OTP. Please try again.", exception.getMessage());
        verify(valueOperations, times(1)).get("forgot:otp:" + testEmail);
        verify(otpService, times(1)).verifyOTP(wrongOTP, correctOTP);
        verify(registerRepository, never()).findById(any(UUID.class));
        // Data should remain in Redis when OTP is invalid
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testVerifyForgotPasswordOTP_NullUserId() {
        // Arrange
        forgotPasswordData.put("userId", null);
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(testEmail, correctOTP);
        });

        assertEquals("Invalid OTP data. Please request a new OTP.", exception.getMessage());
        verify(redisTemplate, times(1)).delete("forgot:otp:" + testEmail);
        verify(registerRepository, never()).findById(any(UUID.class));
    }

    @Test
    void testVerifyForgotPasswordOTP_EmptyUserId() {
        // Arrange
        forgotPasswordData.put("userId", "");
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(testEmail, correctOTP);
        });

        assertEquals("Invalid OTP data. Please request a new OTP.", exception.getMessage());
        verify(redisTemplate, times(1)).delete("forgot:otp:" + testEmail);
    }

    @Test
    void testVerifyForgotPasswordOTP_InvalidUserIdFormat() {
        // Arrange
        forgotPasswordData.put("userId", "invalid-uuid");
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(testEmail, correctOTP);
        });

        assertEquals("Invalid OTP data. Please request a new OTP.", exception.getMessage());
        verify(redisTemplate, times(1)).delete("forgot:otp:" + testEmail);
    }

    @Test
    void testVerifyForgotPasswordOTP_UserNotFound() {
        // Arrange
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);
        when(registerRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(testEmail, correctOTP);
        });

        assertEquals("User not found. Please request a new OTP.", exception.getMessage());
        verify(registerRepository, times(1)).findById(testUserId);
        verify(redisTemplate, times(1)).delete("forgot:otp:" + testEmail);
    }

    @Test
    void testVerifyForgotPasswordOTP_NullEmail() {
        // Arrange
        when(valueOperations.get("forgot:otp:null")).thenReturn(null);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(null, correctOTP);
        });

        assertEquals("OTP expired or invalid. Please request a new OTP.", exception.getMessage());
    }

    @Test
    void testVerifyForgotPasswordOTP_NullOTP() {
        // Arrange
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP(null, correctOTP)).thenReturn(false);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(testEmail, null);
        });

        assertEquals("Invalid OTP. Please try again.", exception.getMessage());
    }

    @Test
    void testVerifyForgotPasswordOTP_UserWithNullFields() {
        // Arrange
        testUser.setFirstname(null);
        testUser.setLastname(null);
        testUser.setUsername(null);

        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP(correctOTP, correctOTP)).thenReturn(true);
        when(registerRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(UUID.class), anyString())).thenReturn("mock-access-token");
        when(jwtUtil.generateRefreshToken(any(UUID.class), anyString())).thenReturn("mock-refresh-token");

        // Act
        Map<String, Object> result = authService.verifyForgotPasswordOTP(testEmail, correctOTP);

        // Assert
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals("", user.get("firstname"));
        assertEquals("", user.get("lastname"));
        assertEquals("", user.get("username"));
    }

    @Test
    void testVerifyForgotPasswordOTP_EmptyOTP() {
        // Arrange
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP("", correctOTP)).thenReturn(false);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(testEmail, "");
        });

        assertEquals("Invalid OTP. Please try again.", exception.getMessage());
    }

    @Test
    void testVerifyForgotPasswordOTP_MissingOTPInData() {
        // Arrange
        forgotPasswordData.remove("otp");
        when(valueOperations.get("forgot:otp:" + testEmail)).thenReturn(forgotPasswordData);
        when(objectMapper.convertValue(eq(forgotPasswordData), any(TypeReference.class)))
            .thenReturn(forgotPasswordData);
        when(otpService.verifyOTP(correctOTP, null)).thenReturn(false);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.verifyForgotPasswordOTP(testEmail, correctOTP);
        });

        assertEquals("Invalid OTP. Please try again.", exception.getMessage());
    }
}

