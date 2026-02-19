package com.sparta.UserService.service;

import com.sparta.UserService.exception.SignupException;
import com.sparta.UserService.model.SignupData;
import com.sparta.UserService.repository.RegisterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupTest {

    @Mock
    private RegisterRepository registerRepository;

    @Mock
    private OTPService otpService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private AuthService authService;

    private String testEmail;
    private String testPassword;
    private String testFirstname;
    private String testLastname;
    private String testUsername;
    private String testOTP;

    @BeforeEach
    void setUp() {
        testEmail = "newuser@example.com";
        testPassword = "password123";
        testFirstname = "Jane";
        testLastname = "Smith";
        testUsername = "janesmith";
        testOTP = "123456";
    }

    @Test
    void testInitiateSignup_Success() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(registerRepository.existsByEmail(testEmail)).thenReturn(false);
        when(otpService.sendOTPByEmail(testEmail)).thenReturn(testOTP);

        // Act
        Map<String, Object> result = authService.initiateSignup(
            testEmail, testPassword, testFirstname, testLastname, testUsername
        );

        // Assert
        assertNotNull(result);
        assertEquals("OTP sent to your email. Please verify within 5 minutes.", result.get("message"));
        assertEquals(testEmail, result.get("email"));

        verify(registerRepository, times(1)).existsByEmail(testEmail);
        verify(otpService, times(1)).sendOTPByEmail(testEmail);
        
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SignupData> dataCaptor = ArgumentCaptor.forClass(SignupData.class);
        ArgumentCaptor<Long> timeoutCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(valueOperations, times(1)).set(
            keyCaptor.capture(),
            dataCaptor.capture(),
            timeoutCaptor.capture(),
            unitCaptor.capture()
        );

        assertEquals("signup:otp:" + testEmail, keyCaptor.getValue());
        SignupData capturedData = dataCaptor.getValue();
        assertEquals(testEmail, capturedData.getEmail());
        assertEquals(testFirstname, capturedData.getFirstname());
        assertEquals(testLastname, capturedData.getLastname());
        assertEquals(testUsername, capturedData.getUsername());
        assertEquals(testOTP, capturedData.getOtp());
        assertNotNull(capturedData.getPassword()); // Password should be hashed
        assertTrue(capturedData.getPassword().startsWith("$2a$")); // BCrypt hash prefix
        assertEquals(5L, timeoutCaptor.getValue());
        assertEquals(TimeUnit.MINUTES, unitCaptor.getValue());
    }

    @Test
    void testInitiateSignup_EmailAlreadyExists() {
        // Arrange
        when(registerRepository.existsByEmail(testEmail)).thenReturn(true);

        // Act & Assert
        SignupException exception = assertThrows(SignupException.class, () -> {
            authService.initiateSignup(testEmail, testPassword, testFirstname, testLastname, testUsername);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(registerRepository, times(1)).existsByEmail(testEmail);
        verify(otpService, never()).sendOTPByEmail(anyString());
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testInitiateSignup_NullEmail() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(registerRepository.existsByEmail(null)).thenReturn(false);
        when(otpService.sendOTPByEmail(null)).thenReturn(testOTP);

        // Act - The method may not throw exception for null email, it will just process it
        Map<String, Object> result = authService.initiateSignup(
            null, testPassword, testFirstname, testLastname, testUsername
        );

        // Assert - Verify it processes without throwing
        assertNotNull(result);
        verify(registerRepository, times(1)).existsByEmail(null);
    }

    @Test
    void testInitiateSignup_EmptyEmail() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(registerRepository.existsByEmail("")).thenReturn(false);
        when(otpService.sendOTPByEmail("")).thenReturn(testOTP);

        // Act
        Map<String, Object> result = authService.initiateSignup(
            "", testPassword, testFirstname, testLastname, testUsername
        );

        // Assert
        assertNotNull(result);
        verify(registerRepository, times(1)).existsByEmail("");
    }

    @Test
    void testInitiateSignup_NullFields() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(registerRepository.existsByEmail(testEmail)).thenReturn(false);
        when(otpService.sendOTPByEmail(testEmail)).thenReturn(testOTP);

        // Act
        Map<String, Object> result = authService.initiateSignup(
            testEmail, testPassword, null, null, null
        );

        // Assert
        assertNotNull(result);
        ArgumentCaptor<SignupData> dataCaptor = ArgumentCaptor.forClass(SignupData.class);
        verify(valueOperations).set(anyString(), dataCaptor.capture(), anyLong(), any(TimeUnit.class));
        
        SignupData capturedData = dataCaptor.getValue();
        assertNull(capturedData.getFirstname());
        assertNull(capturedData.getLastname());
        assertNull(capturedData.getUsername());
    }

    @Test
    void testInitiateSignup_EmptyPassword() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(registerRepository.existsByEmail(testEmail)).thenReturn(false);
        when(otpService.sendOTPByEmail(testEmail)).thenReturn(testOTP);

        // Act
        Map<String, Object> result = authService.initiateSignup(
            testEmail, "", testFirstname, testLastname, testUsername
        );

        // Assert
        assertNotNull(result);
        ArgumentCaptor<SignupData> dataCaptor = ArgumentCaptor.forClass(SignupData.class);
        verify(valueOperations).set(anyString(), dataCaptor.capture(), anyLong(), any(TimeUnit.class));
        
        SignupData capturedData = dataCaptor.getValue();
        assertNotNull(capturedData.getPassword()); // Empty password should still be hashed
    }

    @Test
    void testInitiateSignup_OTPServiceFailure() {
        // Arrange
        when(registerRepository.existsByEmail(testEmail)).thenReturn(false);
        when(otpService.sendOTPByEmail(testEmail)).thenThrow(new RuntimeException("Email service unavailable"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authService.initiateSignup(testEmail, testPassword, testFirstname, testLastname, testUsername);
        });

        verify(registerRepository, times(1)).existsByEmail(testEmail);
        verify(otpService, times(1)).sendOTPByEmail(testEmail);
    }
}

