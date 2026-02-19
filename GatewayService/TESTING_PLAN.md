# Gateway Service - Unit Testing Plan

## Overview
This document outlines the comprehensive unit testing plan for the Gateway Service, including all test files to be created and the specific test cases to be covered.

---

## Test Files Structure

### 1. **JwtUtilTest.java**
**Location:** `src/test/java/com/Sparta/GatewayService/util/JwtUtilTest.java`

**Purpose:** Unit tests for JWT token validation and extraction utilities.

**Test Cases:**
- ✅ `testValidateToken_ValidToken_ReturnsDecodedJWT()` - Valid token should return DecodedJWT
- ✅ `testValidateToken_InvalidToken_ReturnsNull()` - Invalid token should return null
- ✅ `testValidateToken_ExpiredToken_ReturnsNull()` - Expired token should return null
- ✅ `testValidateToken_MalformedToken_ReturnsNull()` - Malformed token should return null
- ✅ `testValidateToken_NullToken_ReturnsNull()` - Null token should return null
- ✅ `testValidateToken_EmptyToken_ReturnsNull()` - Empty token should return null
- ✅ `testGetUserIdFromToken_ValidToken_ReturnsUUID()` - Valid token should extract user ID
- ✅ `testGetUserIdFromToken_InvalidToken_ReturnsNull()` - Invalid token should return null
- ✅ `testGetUserIdFromToken_MissingUserIdClaim_ReturnsNull()` - Token without userId claim should return null
- ✅ `testGetUserIdFromToken_InvalidUUIDFormat_ReturnsNull()` - Invalid UUID format should return null
- ✅ `testGetEmailFromToken_ValidToken_ReturnsEmail()` - Valid token should extract email
- ✅ `testGetEmailFromToken_InvalidToken_ReturnsNull()` - Invalid token should return null
- ✅ `testGetEmailFromToken_MissingEmailClaim_ReturnsNull()` - Token without email claim should return null

---

### 2. **JwtAuthenticationFilterTest.java**
**Location:** `src/test/java/com/Sparta/GatewayService/filter/JwtAuthenticationFilterTest.java`

**Purpose:** Unit tests for JWT authentication filter that validates tokens and protects endpoints.

**Test Cases:**
- ✅ `testFilter_PublicEndpoint_AllowsAccess()` - Public endpoints should bypass authentication
- ✅ `testFilter_AllPublicEndpoints_AllowAccess()` - All defined public endpoints should be accessible
- ✅ `testFilter_ProtectedEndpoint_ValidTokenInCookie_AllowsAccess()` - Valid token in cookie should allow access
- ✅ `testFilter_ProtectedEndpoint_ValidTokenInHeader_AllowsAccess()` - Valid token in Authorization header should allow access
- ✅ `testFilter_ProtectedEndpoint_NoToken_ReturnsUnauthorized()` - Missing token should return 401
- ✅ `testFilter_ProtectedEndpoint_InvalidToken_ReturnsUnauthorized()` - Invalid token should return 401
- ✅ `testFilter_ProtectedEndpoint_ExpiredToken_ReturnsUnauthorized()` - Expired token should return 401
- ✅ `testFilter_ProtectedEndpoint_ValidToken_AddsUserHeaders()` - Valid token should add X-User-Id and X-User-Email headers
- ✅ `testFilter_ProtectedEndpoint_TokenInCookie_PreferredOverHeader()` - Cookie token should be preferred over header token
- ✅ `testFilter_ExtractTokenFromCookie_ValidCookie_ReturnsToken()` - Should extract token from jwt_token cookie
- ✅ `testFilter_ExtractTokenFromCookie_AccessTokenCookie_ReturnsToken()` - Should extract token from access_token cookie
- ✅ `testFilter_ExtractTokenFromCookie_NoCookie_ReturnsNull()` - Missing cookie should return null
- ✅ `testFilter_ExtractTokenFromCookie_MalformedCookie_ReturnsNull()` - Malformed cookie should return null
- ✅ `testFilter_GetOrder_ReturnsCorrectPriority()` - Should return -100 for high priority
- ✅ `testFilter_UnauthorizedResponse_HasCorrectFormat()` - Unauthorized response should have correct JSON format

---

### 3. **JwtCookieFilterTest.java**
**Location:** `src/test/java/com/Sparta/GatewayService/filter/JwtCookieFilterTest.java`

**Purpose:** Unit tests for JWT cookie filter that sets cookies from authentication responses.

**Test Cases:**
- ✅ `testFilter_NonAuthEndpoint_PassesThrough()` - Non-auth endpoints should pass through unchanged
- ✅ `testFilter_AuthEndpoint_WithToken_SetsCookie()` - Auth endpoint with token should set cookie
- ✅ `testFilter_AuthEndpoint_WithoutToken_NoCookie()` - Auth endpoint without token should not set cookie
- ✅ `testFilter_LoginEndpoint_SetsCookie()` - Login endpoint should set cookie
- ✅ `testFilter_VerifySignupOtpEndpoint_SetsCookie()` - Verify signup OTP endpoint should set cookie
- ✅ `testFilter_CookieHasCorrectAttributes()` - Cookie should have HttpOnly, SameSite=Strict, Path=/, Max-Age=86400
- ✅ `testFilter_InvalidJsonResponse_NoCookie()` - Invalid JSON response should not set cookie
- ✅ `testFilter_EmptyResponseBody_NoCookie()` - Empty response body should not set cookie
- ✅ `testFilter_ResponseBodyWithoutToken_NoCookie()` - Response without token field should not set cookie
- ✅ `testFilter_GetOrder_ReturnsCorrectPriority()` - Should return -50 for correct filter order
- ✅ `testFilter_MultipleDataBuffers_CombinesCorrectly()` - Should handle multiple data buffers correctly
- ✅ `testFilter_ExceptionDuringParsing_ContinuesWithoutCookie()` - Exception during parsing should not break filter chain

---

### 4. **GatewayServiceApplicationTest.java**
**Location:** `src/test/java/com/Sparta/GatewayService/GatewayServiceApplicationTests.java`

**Purpose:** Integration test to verify Spring context loads correctly.

**Test Cases:**
- ✅ `testContextLoads()` - Spring application context should load successfully (already exists)

---

## Test Dependencies Required

The following test dependencies should be verified in `pom.xml`:

1. **JUnit 5** (included in spring-boot-starter-test)
2. **Mockito** (included in spring-boot-starter-test)
3. **Reactor Test** (already present)
4. **Spring Boot Test** (already present)
5. **WebFlux Test** (for testing reactive components)

**Note:** May need to add `spring-boot-starter-webflux` test dependency if not already present.

---

## Testing Strategy

### Unit Testing Approach:
1. **JwtUtil**: Mock-free unit tests using real JWT tokens with test secret
2. **JwtAuthenticationFilter**: Mock `JwtUtil` and `GatewayFilterChain`, use `MockServerHttpRequest/Response`
3. **JwtCookieFilter**: Mock `GatewayFilterChain`, use `MockServerHttpRequest/Response` with reactive streams

### Test Data:
- Create helper methods to generate valid/invalid JWT tokens for testing
- Use consistent test secret key across all tests
- Create test fixtures for common scenarios

### Coverage Goals:
- **Line Coverage**: > 90%
- **Branch Coverage**: > 85%
- **Method Coverage**: 100%

---

## Test Execution

### Run all tests:
```bash
mvn test
```

### Run specific test class:
```bash
mvn test -Dtest=JwtUtilTest
```

### Run with coverage (requires JaCoCo plugin):
```bash
mvn clean test jacoco:report
```

---

## Additional Considerations

1. **Integration Tests**: Consider adding integration tests for end-to-end filter chain behavior
2. **Performance Tests**: Consider testing filter performance under load
3. **Security Tests**: Consider adding tests for security edge cases (token tampering, etc.)
4. **Configuration Tests**: Test behavior with different JWT secret configurations

---

## Summary

**Total Test Files:** 4
- JwtUtilTest.java (13 test cases)
- JwtAuthenticationFilterTest.java (16 test cases)
- JwtCookieFilterTest.java (12 test cases)
- GatewayServiceApplicationTests.java (1 test case - existing)

**Total Test Cases:** ~42 unit tests

