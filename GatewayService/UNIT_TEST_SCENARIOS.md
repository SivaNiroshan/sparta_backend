# GatewayService - Unit Test Scenarios Documentation

This document provides a comprehensive overview of all unit test scenarios covered in the GatewayService test suite.

## Test Files Overview

1. **JwtUtilTest.java** - Tests for JWT utility functions
2. **JwtAuthenticationFilterTest.java** - Tests for JWT authentication filter
3. **JwtCookieFilterTest.java** - Tests for JWT cookie management filter
4. **CorsConfigTest.java** - Tests for CORS configuration
5. **GatewayServiceApplicationTests.java** - Application context loading test

---

## 1. JwtUtilTest.java

**Purpose:** Unit tests for JWT token validation and extraction utilities.

**Total Test Cases:** 24

### Test Scenarios Covered:

#### Token Validation (`validateToken`)
1. ✅ **testValidateToken_ValidToken_ReturnsDecodedJWT**
   - **Scenario:** Valid JWT token with correct secret and claims
   - **Expected:** Returns DecodedJWT object with userId and email claims
   - **Coverage:** Happy path - successful token validation

2. ✅ **testValidateToken_InvalidToken_ReturnsNull**
   - **Scenario:** Token signed with different secret key
   - **Expected:** Returns null (token validation fails)
   - **Coverage:** Invalid signature handling

3. ✅ **testValidateToken_ExpiredToken_ReturnsNull**
   - **Scenario:** Token with expiration time in the past
   - **Expected:** Returns null (token expired)
   - **Coverage:** Expiration check

4. ✅ **testValidateToken_MalformedToken_ReturnsNull**
   - **Scenario:** Token with invalid format (not a proper JWT structure)
   - **Expected:** Returns null (malformed token)
   - **Coverage:** Malformed token handling

5. ✅ **testValidateToken_NullToken_ReturnsNull**
   - **Scenario:** Null token input
   - **Expected:** Returns null (null safety)
   - **Coverage:** Null input handling

6. ✅ **testValidateToken_EmptyToken_ReturnsNull**
   - **Scenario:** Empty string token input
   - **Expected:** Returns null (empty input)
   - **Coverage:** Empty input handling

#### User ID Extraction (`getUserIdFromToken`)
7. ✅ **testGetUserIdFromToken_ValidToken_ReturnsUUID**
   - **Scenario:** Valid token with userId claim
   - **Expected:** Returns UUID matching the userId claim
   - **Coverage:** Successful UUID extraction

8. ✅ **testGetUserIdFromToken_InvalidToken_ReturnsNull**
   - **Scenario:** Invalid token (wrong secret)
   - **Expected:** Returns null
   - **Coverage:** Invalid token handling in extraction

9. ✅ **testGetUserIdFromToken_MissingUserIdClaim_ReturnsNull**
   - **Scenario:** Valid token but missing userId claim
   - **Expected:** Returns null (missing claim)
   - **Coverage:** Missing claim handling

10. ✅ **testGetUserIdFromToken_InvalidUUIDFormat_ReturnsNull**
    - **Scenario:** Token with userId claim that is not a valid UUID format
    - **Expected:** Returns null (invalid UUID format)
    - **Coverage:** UUID format validation

#### Email Extraction (`getEmailFromToken`)
11. ✅ **testGetEmailFromToken_ValidToken_ReturnsEmail**
    - **Scenario:** Valid token with email claim
    - **Expected:** Returns email string matching the email claim
    - **Coverage:** Successful email extraction

12. ✅ **testGetEmailFromToken_InvalidToken_ReturnsNull**
    - **Scenario:** Invalid token (wrong secret)
    - **Expected:** Returns null
    - **Coverage:** Invalid token handling in extraction

13. ✅ **testGetEmailFromToken_MissingEmailClaim_ReturnsNull**
    - **Scenario:** Valid token but missing email claim
    - **Expected:** Returns null (missing claim)
    - **Coverage:** Missing claim handling

#### Refresh Token Validation (`validateRefreshToken`)
14. ✅ **testValidateRefreshToken_ValidRefreshToken_ReturnsDecodedJWT**
    - **Scenario:** Valid refresh token with type="refresh" claim
    - **Expected:** Returns DecodedJWT with type="refresh"
    - **Coverage:** Successful refresh token validation

15. ✅ **testValidateRefreshToken_AccessToken_ReturnsNull**
    - **Scenario:** Access token without type="refresh" claim
    - **Expected:** Returns null (not a refresh token)
    - **Coverage:** Token type validation

16. ✅ **testValidateRefreshToken_InvalidToken_ReturnsNull**
    - **Scenario:** Refresh token signed with wrong secret
    - **Expected:** Returns null (invalid signature)
    - **Coverage:** Invalid refresh token handling

17. ✅ **testValidateRefreshToken_ExpiredToken_ReturnsNull**
    - **Scenario:** Expired refresh token
    - **Expected:** Returns null (token expired)
    - **Coverage:** Expired refresh token handling

18. ✅ **testValidateRefreshToken_MalformedToken_ReturnsNull**
    - **Scenario:** Malformed refresh token
    - **Expected:** Returns null (malformed token)
    - **Coverage:** Malformed refresh token handling

19. ✅ **testValidateRefreshToken_NullToken_ReturnsNull**
    - **Scenario:** Null refresh token input
    - **Expected:** Returns null (null safety)
    - **Coverage:** Null input handling

#### Token Expiration Check (`isTokenExpired`)
20. ✅ **testIsTokenExpired_ValidToken_ReturnsFalse**
    - **Scenario:** Valid non-expired token
    - **Expected:** Returns false (token not expired)
    - **Coverage:** Non-expired token check

21. ✅ **testIsTokenExpired_ExpiredToken_ReturnsTrue**
    - **Scenario:** Token with expiration time in the past
    - **Expected:** Returns true (token expired)
    - **Coverage:** Expired token detection

22. ✅ **testIsTokenExpired_MalformedToken_ReturnsTrue**
    - **Scenario:** Malformed token that cannot be decoded
    - **Expected:** Returns true (treated as expired for safety)
    - **Coverage:** Malformed token treated as expired

23. ✅ **testIsTokenExpired_NullToken_ReturnsTrue**
    - **Scenario:** Null token input
    - **Expected:** Returns true (treated as expired for safety)
    - **Coverage:** Null input safety

24. ✅ **testIsTokenExpired_EmptyToken_ReturnsTrue**
    - **Scenario:** Empty string token input
    - **Expected:** Returns true (treated as expired for safety)
    - **Coverage:** Empty input safety

---

## 2. JwtAuthenticationFilterTest.java

**Purpose:** Unit tests for JWT authentication filter that validates tokens and protects endpoints.

**Total Test Cases:** 22

### Test Scenarios Covered:

#### Public Endpoints (No Authentication Required)
1. ✅ **testFilter_PublicEndpoint_AllowsAccess**
   - **Scenario:** Request to `/account/auth/login` without token
   - **Expected:** Request passes through filter chain without validation
   - **Coverage:** Public endpoint bypass

2. ✅ **testFilter_AllPublicEndpoints_AllowAccess**
   - **Scenario:** Requests to all public endpoints without tokens:
     - `/account/auth/login`
     - `/account/auth/signup`
     - `/account/auth/verify-signup-otp`
     - `/account/auth/forgot-password`
     - `/account/auth/verify-forgot-password-otp`
     - `/account/auth/resend-signup-otp`
     - `/account/auth/resend-forgot-password-otp`
     - `/account/auth/email-exists`
   - **Expected:** All requests pass through without validation
   - **Coverage:** Complete public endpoint list validation

3. ✅ **testFilter_RefreshEndpoint_IsPublic**
   - **Scenario:** Request to `/account/auth/refresh` without token
   - **Expected:** Request passes through filter chain
   - **Coverage:** Refresh endpoint is public

4. ✅ **testFilter_LogoutEndpoint_IsPublic**
   - **Scenario:** Request to `/account/auth/logout` without token
   - **Expected:** Request passes through filter chain
   - **Coverage:** Logout endpoint is public

5. ✅ **testFilter_UpdatePasswordEndpoint_IsPublic**
   - **Scenario:** Request to `/account/auth/updatepassword` without token
   - **Expected:** Request passes through filter chain
   - **Coverage:** Update password endpoint is public

#### Protected Endpoints - Valid Token Scenarios
6. ✅ **testFilter_ProtectedEndpoint_ValidTokenInCookie_AllowsAccess**
   - **Scenario:** Protected endpoint with valid JWT token in cookie
   - **Expected:** Request allowed, X-User-Id and X-User-Email headers added
   - **Coverage:** Cookie-based authentication with header injection

7. ✅ **testFilter_ProtectedEndpoint_ValidTokenInHeader_AllowsAccess**
   - **Scenario:** Protected endpoint with valid Bearer token in Authorization header
   - **Expected:** Request allowed, token validated
   - **Coverage:** Header-based authentication

8. ✅ **testFilter_ProtectedEndpoint_ValidToken_AddsUserHeaders**
   - **Scenario:** Valid token adds user information to request headers
   - **Expected:** X-User-Id and X-User-Email headers present in downstream request
   - **Coverage:** User information propagation

9. ✅ **testFilter_ProtectedEndpoint_TokenInCookie_PreferredOverHeader**
   - **Scenario:** Both cookie and header contain tokens
   - **Expected:** Cookie token is used (precedence)
   - **Coverage:** Token source priority

#### Protected Endpoints - Invalid Token Scenarios
10. ✅ **testFilter_ProtectedEndpoint_NoToken_ReturnsUnauthorized**
    - **Scenario:** Protected endpoint without any token
    - **Expected:** Returns 401 Unauthorized with JSON error message
    - **Coverage:** Missing token handling

11. ✅ **testFilter_ProtectedEndpoint_InvalidToken_ReturnsUnauthorized**
    - **Scenario:** Protected endpoint with invalid token
    - **Expected:** Returns 401 Unauthorized
    - **Coverage:** Invalid token rejection

12. ✅ **testFilter_ProtectedEndpoint_ExpiredToken_ReturnsUnauthorized**
    - **Scenario:** Protected endpoint with expired access token and no refresh token
    - **Expected:** Returns 401 Unauthorized
    - **Coverage:** Expired token handling

#### Token Extraction
13. ✅ **testFilter_ExtractTokenFromCookie_ValidCookie_ReturnsToken**
    - **Scenario:** Extract token from jwt_token cookie
    - **Expected:** Token successfully extracted
    - **Coverage:** Cookie token extraction

14. ✅ **testFilter_ExtractTokenFromCookie_AccessTokenCookie_ReturnsToken**
    - **Scenario:** Extract token from access_token cookie
    - **Expected:** Token successfully extracted
    - **Coverage:** Alternative cookie name support

15. ✅ **testFilter_ExtractTokenFromCookie_NoCookie_ReturnsNull**
    - **Scenario:** No cookie header present
    - **Expected:** Returns null, proceeds to check Authorization header
    - **Coverage:** Missing cookie handling

16. ✅ **testFilter_ExtractTokenFromCookie_MalformedCookie_ReturnsNull**
    - **Scenario:** Malformed cookie header
    - **Expected:** Returns null, request rejected
    - **Coverage:** Malformed cookie handling

#### Refresh Token Scenarios
17. ✅ **testFilter_ExpiredToken_WithValidRefreshToken_AllowsAccess**
    - **Scenario:** Expired access token with valid refresh token
    - **Expected:** Request allowed, X-Token-Expired header set to "true", user info from refresh token
    - **Coverage:** Refresh token fallback mechanism

18. ✅ **testFilter_ExpiredToken_WithInvalidRefreshToken_ReturnsUnauthorized**
    - **Scenario:** Expired access token with invalid refresh token
    - **Expected:** Returns 401 Unauthorized
    - **Coverage:** Invalid refresh token rejection

19. ✅ **testFilter_ExpiredToken_NoRefreshToken_ReturnsUnauthorized**
    - **Scenario:** Expired access token without refresh token
    - **Expected:** Returns 401 Unauthorized
    - **Coverage:** Missing refresh token handling

20. ✅ **testFilter_RefreshToken_MissingUserId_ReturnsUnauthorized**
    - **Scenario:** Valid refresh token but missing userId claim
    - **Expected:** Returns 401 Unauthorized
    - **Coverage:** Incomplete refresh token validation

#### Response Format and Filter Configuration
21. ✅ **testFilter_UnauthorizedResponse_HasCorrectFormat**
    - **Scenario:** Unauthorized response format
    - **Expected:** 401 status code, application/json content type, error message in body
    - **Coverage:** Error response format validation

22. ✅ **testFilter_GetOrder_ReturnsCorrectPriority**
    - **Scenario:** Filter order priority
    - **Expected:** Returns -100 (high priority, runs early)
    - **Coverage:** Filter chain ordering

---

## 3. JwtCookieFilterTest.java

**Purpose:** Unit tests for JWT cookie filter that sets cookies from authentication responses.

**Total Test Cases:** 19

### Test Scenarios Covered:

#### Non-Auth Endpoints
1. ✅ **testFilter_NonAuthEndpoint_PassesThrough**
   - **Scenario:** Request to non-authentication endpoint (e.g., `/account/user/profile`)
   - **Expected:** Filter passes through without modifying response
   - **Coverage:** Non-auth endpoint bypass

#### Auth Endpoints - Cookie Setting
2. ✅ **testFilter_AuthEndpoint_WithToken_SetsCookie**
   - **Scenario:** Auth endpoint response contains token in JSON body
   - **Expected:** Sets jwt_token cookie with attributes:
     - HttpOnly flag
     - SameSite=Strict
     - Path=/
     - Max-Age=604800 (7 days)
   - **Coverage:** Cookie creation from response body

3. ✅ **testFilter_AuthEndpoint_WithoutToken_NoCookie**
   - **Scenario:** Auth endpoint response without token field
   - **Expected:** No cookie set
   - **Coverage:** Missing token handling

4. ✅ **testFilter_LoginEndpoint_SetsCookie**
   - **Scenario:** `/account/auth/login` endpoint returns token
   - **Expected:** Sets jwt_token cookie
   - **Coverage:** Login endpoint cookie setting

5. ✅ **testFilter_VerifySignupOtpEndpoint_SetsCookie**
   - **Scenario:** `/account/auth/verify-signup-otp` endpoint returns token
   - **Expected:** Sets jwt_token cookie
   - **Coverage:** Signup verification cookie setting

6. ✅ **testFilter_VerifyForgotPasswordOtpEndpoint_SetsCookie**
   - **Scenario:** `/account/auth/verify-forgot-password-otp` endpoint returns token
   - **Expected:** Sets jwt_token cookie
   - **Coverage:** Password reset verification cookie setting

7. ✅ **testFilter_RefreshEndpoint_SetsCookie**
   - **Scenario:** `/account/auth/refresh` endpoint returns token
   - **Expected:** Sets jwt_token cookie
   - **Coverage:** Refresh endpoint cookie setting

#### Cookie Attributes Validation
8. ✅ **testFilter_CookieHasCorrectAttributes**
   - **Scenario:** Verify all cookie attributes are set correctly
   - **Expected:** Cookie contains:
     - jwt_token value
     - Path=/
     - HttpOnly flag
     - SameSite=Strict
     - Max-Age=604800 (7 days)
   - **Coverage:** Complete cookie attribute validation

#### Refresh Token Cookie
9. ✅ **testFilter_AuthEndpoint_WithRefreshToken_SetsBothCookies**
   - **Scenario:** Response contains both token and refreshToken fields
   - **Expected:** Sets both jwt_token and refresh_token cookies
   - **Coverage:** Dual cookie setting

#### Error Scenarios
10. ✅ **testFilter_InvalidJsonResponse_NoCookie**
    - **Scenario:** Response body is not valid JSON
    - **Expected:** No cookie set, filter continues without error
    - **Coverage:** Invalid JSON handling

11. ✅ **testFilter_EmptyResponseBody_NoCookie**
    - **Scenario:** Empty response body
    - **Expected:** No cookie set
    - **Coverage:** Empty response handling

12. ✅ **testFilter_ResponseBodyWithoutToken_NoCookie**
    - **Scenario:** Valid JSON but missing token field
    - **Expected:** No cookie set
    - **Coverage:** Missing token field handling

#### Logout Scenarios
13. ✅ **testFilter_LogoutEndpoint_ClearsCookies**
    - **Scenario:** Request to `/account/auth/logout` endpoint
    - **Expected:** Clears all cookies (jwt_token, access_token, refresh_token) with Max-Age=0
    - **Coverage:** Cookie clearing on logout

14. ✅ **testFilter_LogoutEndpoint_AddsTokensToHeaders**
    - **Scenario:** Logout request with tokens in cookies
    - **Expected:** Adds X-Access-Token and X-Refresh-Token headers for blacklisting
    - **Coverage:** Token extraction for logout

15. ✅ **testFilter_LogoutEndpoint_NoTokens_StillClearsCookies**
    - **Scenario:** Logout request without tokens
    - **Expected:** Still clears cookies (defensive programming)
    - **Coverage:** Logout without tokens

16. ✅ **testFilter_LogoutEndpoint_ExtractsAccessTokenFromAccessTokenCookie**
    - **Scenario:** Logout with access_token cookie (alternative name)
    - **Expected:** Correctly extracts token from access_token cookie
    - **Coverage:** Alternative cookie name support in logout

#### Edge Cases
17. ✅ **testFilter_MultipleDataBuffers_CombinesCorrectly**
    - **Scenario:** Response body split across multiple data buffers
    - **Expected:** Correctly combines buffers and extracts token
    - **Coverage:** Multi-buffer response handling

18. ✅ **testFilter_ExceptionDuringParsing_ContinuesWithoutCookie**
    - **Scenario:** Exception occurs during JSON parsing
    - **Expected:** Filter continues without setting cookie, no exception thrown
    - **Coverage:** Exception handling and graceful degradation

19. ✅ **testFilter_GetOrder_ReturnsCorrectPriority**
    - **Scenario:** Filter order priority
    - **Expected:** Returns -50 (runs after JwtAuthenticationFilter but before routing)
    - **Coverage:** Filter chain ordering

---

## 4. CorsConfigTest.java

**Purpose:** Unit tests for CORS configuration and filter behavior.

**Total Test Cases:** 11

### Test Scenarios Covered:

#### Configuration
1. ✅ **testCorsWebFilter_BeanCreated**
   - **Scenario:** CORS filter bean creation
   - **Expected:** CorsWebFilter bean created successfully
   - **Coverage:** Bean configuration validation

#### Allowed Origins
2. ✅ **testCorsFilter_AllowedOrigins_AllowsLocalhostPorts**
   - **Scenario:** Requests from various localhost origins
   - **Expected:** All localhost ports allowed:
     - `http://localhost:3000`
     - `http://localhost:5173`
     - `http://localhost:5174`
     - `http://localhost:5175`
     - `http://localhost:8080`
     - `http://127.0.0.1:3000`
     - `http://127.0.0.1:5173`
     - `http://127.0.0.1:5174`
     - `http://127.0.0.1:5175`
   - **Coverage:** Origin whitelist validation

#### HTTP Methods
3. ✅ **testCorsFilter_AllHttpMethods_Allowed**
   - **Scenario:** Requests with different HTTP methods
   - **Expected:** All methods allowed: GET, POST, PUT, PATCH, DELETE, OPTIONS
   - **Coverage:** HTTP method whitelist validation

#### Preflight Requests
4. ✅ **testCorsFilter_OptionsRequest_HandlesPreflight**
   - **Scenario:** OPTIONS preflight request with CORS headers
   - **Expected:** Preflight request handled correctly
   - **Coverage:** CORS preflight handling

#### Request Types
5. ✅ **testCorsFilter_GetRequest_AllowsCors**
   - **Scenario:** GET request with Origin header
   - **Expected:** Request processed with CORS headers
   - **Coverage:** GET request CORS support

6. ✅ **testCorsFilter_PostRequest_AllowsCors**
   - **Scenario:** POST request with Origin header
   - **Expected:** Request processed with CORS headers
   - **Coverage:** POST request CORS support

#### CORS Features
7. ✅ **testCorsFilter_Credentials_Allowed**
   - **Scenario:** Request with cookies/credentials
   - **Expected:** Credentials allowed (allowCredentials=true)
   - **Coverage:** Credential support validation

8. ✅ **testCorsFilter_AllHeaders_Allowed**
   - **Scenario:** Request with various headers (Authorization, Content-Type, custom headers)
   - **Expected:** All headers allowed
   - **Coverage:** Header whitelist validation

9. ✅ **testCorsFilter_ExposedHeaders_Configured**
   - **Scenario:** Response headers exposure
   - **Expected:** Exposed headers configured: Authorization, Content-Type, X-Total-Count
   - **Coverage:** Exposed headers configuration

10. ✅ **testCorsFilter_MaxAge_Configured**
    - **Scenario:** Preflight cache configuration
    - **Expected:** Max-Age configured to 3600 seconds (1 hour)
    - **Coverage:** Preflight cache configuration

#### Path Coverage
11. ✅ **testCorsFilter_AllPaths_Configured**
    - **Scenario:** CORS applied to various paths
    - **Expected:** CORS applied to all paths (`/**`)
    - **Coverage:** Path pattern validation

---

## 5. GatewayServiceApplicationTests.java

**Purpose:** Integration test to verify Spring application context loads correctly.

**Total Test Cases:** 1

### Test Scenarios Covered:

1. ✅ **testContextLoads**
   - **Scenario:** Spring Boot application context initialization
   - **Expected:** Application context loads successfully without errors
   - **Coverage:** Application startup validation, bean creation, configuration loading

---

## Summary Statistics

| Test File | Test Cases | Coverage Focus |
|-----------|------------|----------------|
| JwtUtilTest | 24 | Token validation, extraction, refresh tokens, expiration |
| JwtAuthenticationFilterTest | 22 | Authentication, public/protected endpoints, refresh tokens |
| JwtCookieFilterTest | 19 | Cookie management, logout, error handling |
| CorsConfigTest | 11 | CORS configuration, origins, methods, preflight |
| GatewayServiceApplicationTests | 1 | Application context loading |
| **Total** | **77** | **Comprehensive coverage** |

---

## Test Execution

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=JwtUtilTest
mvn test -Dtest=JwtAuthenticationFilterTest
mvn test -Dtest=JwtCookieFilterTest
mvn test -Dtest=CorsConfigTest
mvn test -Dtest=GatewayServiceApplicationTests
```

### Run with Coverage (requires JaCoCo plugin)
```bash
mvn clean test jacoco:report
```

---

## Test Coverage Goals

- **Line Coverage:** > 90%
- **Branch Coverage:** > 85%
- **Method Coverage:** 100%

---

## Key Testing Patterns Used

1. **Given-When-Then Structure** - All tests follow BDD-style structure
2. **Mockito for Mocking** - Used for mocking dependencies (JwtUtil, GatewayFilterChain)
3. **Reactor Test** - StepVerifier used for testing reactive Mono/Flux streams
4. **Spring Mock Objects** - MockServerHttpRequest/Response for testing web components
5. **ReflectionTestUtils** - Used to inject test values into Spring components

---

## Edge Cases Covered

- Null and empty inputs
- Malformed tokens and cookies
- Expired tokens
- Missing claims
- Invalid formats
- Exception handling
- Multiple data buffers
- Token refresh scenarios
- Logout scenarios
- CORS preflight requests
- Missing tokens
- Invalid JSON responses
- Empty response bodies

---

## Notes

- All tests use a consistent test secret key for JWT operations: `test-secret-key-for-jwt-testing-min-256-bits-required-for-hmac256-algorithm`
- Tests are isolated and do not require external services
- Reactive programming patterns are properly tested using StepVerifier
- Cookie parsing and header manipulation are thoroughly tested
- CORS configuration is validated for all supported origins and methods
- Cookie Max-Age is set to 604800 seconds (7 days) for access tokens
- Refresh token cookies also use Max-Age=604800 seconds (7 days)
- Filter order: JwtAuthenticationFilter (-100) runs before JwtCookieFilter (-50)
