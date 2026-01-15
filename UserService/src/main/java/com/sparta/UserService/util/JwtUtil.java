package com.sparta.UserService.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret:your-secret-key-change-this-in-production-min-256-bits}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // Default 24 hours in milliseconds
    private long expiration;

    @Value("${jwt.refresh.expiration:604800000}") // Default 7 days in milliseconds
    private long refreshExpiration;

    /**
     * Generates a JWT token for a user
     * @param userId User ID
     * @param email User email
     * @return JWT token string
     */
    public String generateToken(UUID userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return JWT.create()
                .withSubject(userId.toString())
                .withClaim("userId", userId.toString())
                .withClaim("email", email)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * Validates a JWT token (without checking blacklist - blacklist check should be done separately)
     * @param token JWT token string
     * @return DecodedJWT if valid, null otherwise
     */
    public DecodedJWT validateToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Decodes a JWT token without validation (useful for extracting expiration)
     * @param token JWT token string
     * @return DecodedJWT if decodable, null otherwise
     */
    public DecodedJWT decodeToken(String token) {
        try {
            return JWT.decode(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts user ID from JWT token
     * @param token JWT token string
     * @return User ID as UUID, null if invalid
     */
    public UUID getUserIdFromToken(String token) {
        try {
            DecodedJWT decoded = validateToken(token);
            if (decoded != null) {
                String userIdStr = decoded.getClaim("userId").asString();
                return UUID.fromString(userIdStr);
            }
        } catch (Exception e) {
            // Invalid token
        }
        return null;
    }

    /**
     * Extracts email from JWT token
     * @param token JWT token string
     * @return Email string, null if invalid
     */
    public String getEmailFromToken(String token) {
        try {
            DecodedJWT decoded = validateToken(token);
            if (decoded != null) {
                return decoded.getClaim("email").asString();
            }
        } catch (Exception e) {
            // Invalid token
        }
        return null;
    }

    /**
     * Generates a refresh token for a user (longer expiration - 7 days)
     * @param userId User ID
     * @param email User email
     * @return Refresh JWT token string
     */
    public String generateRefreshToken(UUID userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return JWT.create()
                .withSubject(userId.toString())
                .withClaim("userId", userId.toString())
                .withClaim("email", email)
                .withClaim("type", "refresh") // Mark as refresh token
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * Validates a refresh token
     * @param token Refresh token string
     * @return DecodedJWT if valid refresh token, null otherwise
     */
    public DecodedJWT validateRefreshToken(String token) {
        try {
            DecodedJWT decoded = validateToken(token);
            if (decoded != null) {
                // Check if it's a refresh token
                String type = decoded.getClaim("type").asString();
                if ("refresh".equals(type)) {
                    return decoded;
                }
            }
        } catch (Exception e) {
            // Invalid token
        }
        return null;
    }
}


