package com.sparta.UserService.service;

import com.sparta.UserService.exception.ForgotException;
import com.sparta.UserService.model.UserDetails;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordTest {

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
    private String testOTP;
    private UserDetails testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testEmail = "user@example.com";
        testOTP = "654321";
        testUserId = UUID.randomUUID();

        testUser = new UserDetails();
        testUser.setId(testUserId);
        testUser.setEmail(testEmail);
        testUser.setFirstname("John");
        testUser.setLastname("Doe");
        testUser.setUsername("johndoe");
    }

    @Test
    void testInitiateForgotPassword_Success() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(registerRepository.findByEmail(testEmail)).thenReturn(testUser);
        when(otpService.sendOTPByEmail(eq(testEmail), anyString(), anyString())).thenReturn(testOTP);

        // Act
        Map<String, Object> result = authService.initiateForgotPassword(testEmail);

        // Assert
        assertNotNull(result);
        assertEquals("OTP sent to your email. Please verify within 5 minutes.", result.get("message"));
        assertEquals(testEmail, result.get("email"));

        verify(registerRepository, times(1)).findByEmail(testEmail);
        verify(otpService, times(1)).sendOTPByEmail(
            eq(testEmail),
            eq("Password Reset OTP"),
            eq("You have requested to reset your password. Use the OTP below to verify your identity.")
        );

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> dataCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Long> timeoutCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(valueOperations, times(1)).set(
            keyCaptor.capture(),
            dataCaptor.capture(),
            timeoutCaptor.capture(),
            unitCaptor.capture()
        );

        assertEquals("forgot:otp:" + testEmail, keyCaptor.getValue());
        @SuppressWarnings("unchecked")
        Map<String, String> capturedData = dataCaptor.getValue();
        assertEquals(testUserId.toString(), capturedData.get("userId"));
        assertEquals(testEmail, capturedData.get("email"));
        assertEquals(testOTP, capturedData.get("otp"));
        assertNotNull(capturedData.get("createdAt"));
        assertEquals(5L, timeoutCaptor.getValue());
        assertEquals(TimeUnit.MINUTES, unitCaptor.getValue());
    }

    @Test
    void testInitiateForgotPassword_EmailDoesNotExist() {
        // Arrange
        when(registerRepository.findByEmail(testEmail)).thenReturn(null);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.initiateForgotPassword(testEmail);
        });

        assertEquals("Email does not exist", exception.getMessage());
        verify(registerRepository, times(1)).findByEmail(testEmail);
        verify(otpService, never()).sendOTPByEmail(anyString(), anyString(), anyString());
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testInitiateForgotPassword_NullEmail() {
        // Arrange
        when(registerRepository.findByEmail(null)).thenReturn(null);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.initiateForgotPassword(null);
        });

        assertEquals("Email does not exist", exception.getMessage());
        verify(registerRepository, times(1)).findByEmail(null);
    }

    @Test
    void testInitiateForgotPassword_EmptyEmail() {
        // Arrange
        when(registerRepository.findByEmail("")).thenReturn(null);

        // Act & Assert
        ForgotException exception = assertThrows(ForgotException.class, () -> {
            authService.initiateForgotPassword("");
        });

        assertEquals("Email does not exist", exception.getMessage());
        verify(registerRepository, times(1)).findByEmail("");
    }

    @Test
    void testInitiateForgotPassword_UserWithNullFields() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserDetails userWithNulls = new UserDetails();
        userWithNulls.setId(testUserId);
        userWithNulls.setEmail(testEmail);
        userWithNulls.setFirstname(null);
        userWithNulls.setLastname(null);
        userWithNulls.setUsername(null);

        when(registerRepository.findByEmail(testEmail)).thenReturn(userWithNulls);
        when(otpService.sendOTPByEmail(eq(testEmail), anyString(), anyString())).thenReturn(testOTP);

        // Act
        Map<String, Object> result = authService.initiateForgotPassword(testEmail);

        // Assert
        assertNotNull(result);
        assertEquals(testEmail, result.get("email"));
        verify(registerRepository, times(1)).findByEmail(testEmail);
    }

    @Test
    void testInitiateForgotPassword_OTPServiceFailure() {
        // Arrange
        when(registerRepository.findByEmail(testEmail)).thenReturn(testUser);
        when(otpService.sendOTPByEmail(eq(testEmail), anyString(), anyString()))
            .thenThrow(new RuntimeException("Email service unavailable"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authService.initiateForgotPassword(testEmail);
        });

        verify(registerRepository, times(1)).findByEmail(testEmail);
        verify(otpService, times(1)).sendOTPByEmail(eq(testEmail), anyString(), anyString());
    }

    @Test
    void testInitiateForgotPassword_RedisStorageFailure() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(registerRepository.findByEmail(testEmail)).thenReturn(testUser);
        when(otpService.sendOTPByEmail(eq(testEmail), anyString(), anyString())).thenReturn(testOTP);
        doThrow(new RuntimeException("Redis unavailable")).when(valueOperations)
            .set(anyString(), any(), anyLong(), any(TimeUnit.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authService.initiateForgotPassword(testEmail);
        });

        verify(registerRepository, times(1)).findByEmail(testEmail);
        verify(otpService, times(1)).sendOTPByEmail(eq(testEmail), anyString(), anyString());
    }
}

