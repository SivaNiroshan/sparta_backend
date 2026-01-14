# Gateway Service Filter Flow Explanation

## How Filters Are Automatically Called

### 1. Application Startup
When `GatewayServiceApplication` starts:
```java
@SpringBootApplication  // This annotation enables component scanning
public class GatewayServiceApplication {
    // Spring automatically finds:
    // - @Component classes
    // - Classes implementing GlobalFilter
    // - Registers them in the filter chain
}
```

### 2. Filter Registration
Spring Cloud Gateway automatically discovers:
- `JwtAuthenticationFilter` (implements `GlobalFilter`, annotated with `@Component`)
- `JwtCookieFilter` (implements `GlobalFilter`, annotated with `@Component`)

## Request Flow Example

### Example 1: Login Request (Public Endpoint)

```
Request: POST http://localhost:8000/account/auth/login
Body: { "email": "user@example.com", "password": "pass123" }
```

**Step-by-Step Execution:**

1. **JwtAuthenticationFilter.filter()** (Order: -100)
   ```java
   String path = "/account/auth/login";
   if (isPublicEndpoint(path)) {  // Returns TRUE
       return chain.filter(exchange);  // ✅ Passes through
   }
   ```

2. **JwtCookieFilter.filter()** (Order: -50)
   ```java
   String path = "/account/auth/login";
   if (isAuthEndpoint(path)) {  // Returns TRUE
       // Wraps response to intercept it later
       return chain.filter(exchange.mutate().response(decoratedResponse).build());
   }
   ```

3. **Spring Cloud Gateway Route Matching**
   ```properties
   # From application.properties:
   spring.cloud.gateway.routes[0].predicates[0]=Path=/account/**
   spring.cloud.gateway.routes[0].uri=http://localhost:8081
   ```
   - Matches: `/account/**` pattern
   - Routes to: `http://localhost:8081`

4. **HTTP Request Forwarded to UserService**
   ```
   POST http://localhost:8081/account/auth/login
   Headers: Content-Type: application/json
   Body: { "email": "user@example.com", "password": "pass123" }
   ```

5. **UserService Response**
   ```json
   {
     "user": { "id": "...", "email": "..." },
     "message": "Login successful",
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   }
   ```

6. **JwtCookieFilter Intercepts Response**
   ```java
   // In JwtCookieFilter.writeWith()
   JsonNode jsonNode = objectMapper.readTree(responseBody);
   if (jsonNode.has("token")) {
       String token = jsonNode.get("token").asText();
       // Sets cookie
       getHeaders().add(HttpHeaders.SET_COOKIE, 
           "jwt_token=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=86400");
   }
   ```

7. **Client Receives Response**
   - Response body: Same JSON from UserService
   - Set-Cookie header: `jwt_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

---

### Example 2: Protected Endpoint Request

```
Request: GET http://localhost:8000/account/user/profile
Cookie: jwt_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Step-by-Step Execution:**

1. **JwtAuthenticationFilter.filter()** (Order: -100)
   ```java
   String path = "/account/user/profile";
   if (isPublicEndpoint(path)) {  // Returns FALSE
       // Not public, need to validate token
   }
   
   String token = extractTokenFromCookie(request);  // Extracts from cookie
   if (jwtUtil.validateToken(token) == null) {  // Validates token
       return handleUnauthorized(exchange);  // ❌ Returns 401 if invalid
   }
   
   // ✅ Token is valid, add user info to headers
   ServerHttpRequest modifiedRequest = request.mutate()
       .header("X-User-Id", "user-uuid-here")
       .header("X-User-Email", "user@example.com")
       .build();
   
   return chain.filter(exchange.mutate().request(modifiedRequest).build());
   ```

2. **JwtCookieFilter.filter()** (Order: -50)
   ```java
   String path = "/account/user/profile";
   if (isAuthEndpoint(path)) {  // Returns FALSE
       return chain.filter(exchange);  // ✅ Passes through (no cookie setting needed)
   }
   ```

3. **Spring Cloud Gateway Route Matching**
   - Matches: `/account/**` pattern
   - Routes to: `http://localhost:8081`

4. **HTTP Request Forwarded to UserService**
   ```
   GET http://localhost:8081/account/user/profile
   Headers: 
     - Cookie: jwt_token=...
     - X-User-Id: user-uuid-here
     - X-User-Email: user@example.com
   ```

5. **UserService Processes Request**
   - Can use `X-User-Id` header to identify user
   - Returns user profile data

6. **Response Back to Client**
   - No cookie modification (not an auth endpoint)
   - Returns profile data

---

## Key Concepts

### 1. Filter Chain (`chain.filter()`)
```java
return chain.filter(exchange);
```
- Calls the **next filter** in the chain
- If it's the last filter, routes the request to the target service
- This is how filters pass control to each other

### 2. Exchange Mutation
```java
exchange.mutate().request(modifiedRequest).build()
```
- Creates a new exchange with modified request
- Used to add headers, modify request, etc.

### 3. Response Interception (JwtCookieFilter)
```java
ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(...) {
    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        // Intercepts response body
        // Modifies headers (adds cookie)
        // Returns modified response
    }
};
```

### 4. Route Matching
Spring Cloud Gateway matches routes based on:
- **Predicates**: `Path=/account/**` matches any path starting with `/account/`
- **URI**: Where to forward the request (`http://localhost:8081`)

## Filter Order Summary

| Filter | Order | Purpose | When It Runs |
|--------|-------|---------|--------------|
| JwtAuthenticationFilter | -100 | Validates JWT for protected endpoints | Every request (early) |
| JwtCookieFilter | -50 | Sets cookies on auth responses | Every request (after auth) |
| Route Filters | 0+ | Routes to target services | After all global filters |

## Important Notes

1. **Filters are called automatically** - No manual invocation needed
2. **Order matters** - Lower number = runs earlier
3. **chain.filter()** - Must be called to continue the chain
4. **Exchange is immutable** - Use `mutate()` to create modified versions
5. **Routing happens automatically** - After filters, Gateway routes based on predicates

