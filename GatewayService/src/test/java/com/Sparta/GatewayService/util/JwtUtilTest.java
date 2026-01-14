package com.Sparta.GatewayService.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-min-256-bits-required-for-hmac256-algorithm";
    private static final String INVALID_SECRET = "different-secret-key-for-jwt-testing-min-256-bits-required-for-hmac256-algorithm";
    
    private UUID testUserId;
    private String testEmail;

    @BeforeEach
    void setUp() {
        // Set the secret using reflection
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
    }

    private String createValidToken(UUID userId, String email) {
        return JWT.create()
                .withClaim("userId", userId.toString())
                .withClaim("email", email)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .sign(Algorithm.HMAC256(TEST_SECRET));
    }

    private String createExpiredToken(UUID userId, String email) {
        return JWT.create()
                .withClaim("userId", userId.toString())
                .withClaim("email", email)
                .withExpiresAt(new Date(System.currentTimeMillis() - 3600000)) // Expired 1 hour ago
                .sign(Algorithm.HMAC256(TEST_SECRET));
    }

    private String createTokenWithInvalidSecret(UUID userId, String email) {
        return JWT.create()
                .withClaim("userId", userId.toString())
                .withClaim("email", email)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(INVALID_SECRET));
    }

    @Test
    void testValidateToken_ValidToken_ReturnsDecodedJWT() {
        // Given
        String token = createValidToken(testUserId, testEmail);

        // When
        DecodedJWT result = jwtUtil.validateToken(token);

        // Then
        assertNotNull(result);
        assertEquals(testUserId.toString(), result.getClaim("userId").asString());
        assertEquals(testEmail, result.getClaim("email").asString());
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsNull() {
        // Given
        String token = createTokenWithInvalidSecret(testUserId, testEmail);

        // When
        DecodedJWT result = jwtUtil.validateToken(token);

        // Then
        assertNull(result);
    }

    @Test
    void testValidateToken_ExpiredToken_ReturnsNull() {
        // Given
        String token = createExpiredToken(testUserId, testEmail);

        // When
        DecodedJWT result = jwtUtil.validateToken(token);

        // Then
        assertNull(result);
    }

    @Test
    void testValidateToken_MalformedToken_ReturnsNull() {
        // Given
        String token = "malformed.token.here";

        // When
        DecodedJWT result = jwtUtil.validateToken(token);

        // Then
        assertNull(result);
    }

    @Test
    void testValidateToken_NullToken_ReturnsNull() {
        // When
        DecodedJWT result = jwtUtil.validateToken(null);

        // Then
        assertNull(result);
    }

    @Test
    void testValidateToken_EmptyToken_ReturnsNull() {
        // When
        DecodedJWT result = jwtUtil.validateToken("");

        // Then
        assertNull(result);
    }

    @Test
    void testGetUserIdFromToken_ValidToken_ReturnsUUID() {
        // Given
        String token = createValidToken(testUserId, testEmail);

        // When
        UUID result = jwtUtil.getUserIdFromToken(token);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result);
    }

    @Test
    void testGetUserIdFromToken_InvalidToken_ReturnsNull() {
        // Given
        String token = createTokenWithInvalidSecret(testUserId, testEmail);

        // When
        UUID result = jwtUtil.getUserIdFromToken(token);

        // Then
        assertNull(result);
    }

    @Test
    void testGetUserIdFromToken_MissingUserIdClaim_ReturnsNull() {
        // Given
        String token = JWT.create()
                .withClaim("email", testEmail)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        // When
        UUID result = jwtUtil.getUserIdFromToken(token);

        // Then
        assertNull(result);
    }

    @Test
    void testGetUserIdFromToken_InvalidUUIDFormat_ReturnsNull() {
        // Given
        String token = JWT.create()
                .withClaim("userId", "not-a-valid-uuid")
                .withClaim("email", testEmail)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        // When
        UUID result = jwtUtil.getUserIdFromToken(token);

        // Then
        assertNull(result);
    }

    @Test
    void testGetEmailFromToken_ValidToken_ReturnsEmail() {
        // Given
        String token = createValidToken(testUserId, testEmail);

        // When
        String result = jwtUtil.getEmailFromToken(token);

        // Then
        assertNotNull(result);
        assertEquals(testEmail, result);
    }

    @Test
    void testGetEmailFromToken_InvalidToken_ReturnsNull() {
        // Given
        String token = createTokenWithInvalidSecret(testUserId, testEmail);

        // When
        String result = jwtUtil.getEmailFromToken(token);

        // Then
        assertNull(result);
    }

    @Test
    void testGetEmailFromToken_MissingEmailClaim_ReturnsNull() {
        // Given
        String token = JWT.create()
                .withClaim("userId", testUserId.toString())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        // When
        String result = jwtUtil.getEmailFromToken(token);

        // Then
        assertNull(result);
    }
}

