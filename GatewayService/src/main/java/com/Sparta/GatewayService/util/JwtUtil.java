package com.Sparta.GatewayService.util;

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

    /**
     * Validates a JWT token
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

    /**
     * Checks if a token is expired (without throwing exception)
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decoded = JWT.decode(token);
            return decoded.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true; // If we can't decode, consider it expired
        }
    }
}


