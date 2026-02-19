# Gateway Service API Documentation

## Overview
The Gateway Service uses **SpringDoc OpenAPI** (Swagger 3) for API documentation. This gateway acts as a single entry point for all microservices, handling routing, authentication, CORS, and request forwarding.

## Accessing Swagger UI

Once the Gateway Service is running, you can access the Swagger UI at:

**Local Development:**
- Swagger UI: `http://localhost:8000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8000/v3/api-docs`
- OpenAPI YAML: `http://localhost:8000/v3/api-docs.yaml`

## Gateway Service Features

### 1. Request Routing
The gateway routes incoming requests to the appropriate microservice based on URL patterns:
- `/account/**` → UserService (port 8081)
- `/upload/**` → UploadService (port 8082)
- `/stream/**` → StreamingService (port 8083)

### 2. JWT Authentication
- Validates JWT tokens for protected endpoints
- Extracts tokens from Authorization header or cookies
- Public endpoints bypass authentication (login, signup, etc.)

### 3. CORS Support
- Cross-Origin Resource Sharing enabled
- Configurable allowed origins, methods, and headers

### 4. Cookie Handling
- Automatic JWT cookie extraction
- Cookie-based authentication support

## Gateway Service Endpoints

### Gateway Information
- `GET /api/gateway/info` - Get gateway service information, routes, and features

## Routes Through Gateway

All requests to the gateway are forwarded to the appropriate microservice. The following routes are available:

---

## UserService Routes (`/account/**`)

### Authentication Endpoints (`/account/auth`)
- `POST /account/auth/login` - User login (Public)
- `POST /account/auth/signup` - User registration (Public)
- `POST /account/auth/verify-signup-otp` - Verify signup OTP (Public)
- `GET /account/auth/email-exists` - Check if email exists (Public)
- `PATCH /account/auth/updatepassword` - Update password (Protected)
- `POST /account/auth/forgot-password` - Initiate password reset (Public)
- `POST /account/auth/verify-forgot-password-otp` - Verify password reset OTP (Public)
- `POST /account/auth/resend-signup-otp` - Resend signup OTP (Public)
- `POST /account/auth/resend-forgot-password-otp` - Resend password reset OTP (Public)
- `POST /account/auth/refresh` - Refresh access token (Protected)
- `POST /account/auth/logout` - User logout (Protected)

### Friend Management Endpoints (`/account/friend`)
- `POST /account/friend/add` - Add friend or send friend request (Protected)
- `GET /account/friend/current` - Get current friends (Protected)
- `GET /account/friend/blocked` - Get blocked friends (Protected)
- `GET /account/friend/sent-requests` - Get sent friend requests (Protected)
- `GET /account/friend/received-requests` - Get received friend requests (Protected)
- `POST /account/friend/accept` - Accept friend request (Protected)
- `POST /account/friend/reject` - Reject friend request (Protected)
- `POST /account/friend/cancel-request` - Cancel sent friend request (Protected)
- `POST /account/friend/block` - Block a friend (Protected)
- `POST /account/friend/unblock` - Unblock a user (Protected)
- `DELETE /account/friend/remove` - Remove a friend (Protected)
- `GET /account/friend/search` - Search friends (Protected)
- `GET /account/friend/get` - Get friends (Legacy endpoint) (Protected)

### User Profile Endpoints (`/account/user`)
- `GET /account/user/profile` - Get user profile (Protected)
- `PATCH /account/user/profile` - Update user profile (Protected)

---

## UploadService Routes (`/upload/**`)

### File Upload Endpoints (TUS Protocol)
- `OPTIONS /upload` - TUS protocol options (Public)
- `POST /upload` - Initiate file upload (Public)
- `OPTIONS /upload/{uuid}` - TUS protocol options for specific upload (Public)
- `HEAD /upload/{uuid}` - Get upload status (Public)
- `PATCH /upload/{uuid}` - Upload file chunk (Public)
- `POST /upload/{uuid}/pause` - Pause upload (Public)
- `POST /upload/{uuid}/resume` - Resume upload (Public)
- `POST /upload/{uuid}/cancel` - Cancel upload (Public)

**Note:** UploadService uses the TUS (Tus Resumable Upload Protocol) for resumable file uploads.

---

## StreamingService Routes (`/stream/**`)

### Media Streaming Endpoints
- `GET /stream/{videoId}/manifest.mpd` - Get DASH manifest file (Public)
- `GET /stream/{videoId}/**` - Stream video files (segments, audio, etc.) (Public)
- `GET /stream/bandwidth` - Get bandwidth measurement (Public)

**Note:** StreamingService provides adaptive video streaming using DASH (Dynamic Adaptive Streaming over HTTP).

---

## Using Swagger UI

1. **Navigate to Swagger UI**: Open `http://localhost:8000/swagger-ui.html` in your browser

2. **Explore APIs**: 
   - APIs are grouped by tags (Gateway Service, Authentication, Friend Management, etc.)
   - Click on any endpoint to expand and see details

3. **Test an Endpoint**:
   - Click "Try it out" button
   - Fill in the required parameters
   - For protected endpoints, add JWT token in Authorization header:
     ```
     Authorization: Bearer <your-jwt-token>
     ```
   - Click "Execute"
   - View the response below

4. **View Models**: 
   - Scroll down to see all request/response models
   - Click on any model to see its schema

## Authentication

### Public Endpoints
The following endpoints do not require authentication:
- `/account/auth/login`
- `/account/auth/signup`
- `/account/auth/verify-signup-otp`
- `/account/auth/email-exists`
- `/account/auth/forgot-password`
- `/account/auth/verify-forgot-password-otp`
- `/account/auth/resend-signup-otp`
- `/account/auth/resend-forgot-password-otp`
- `/upload/**` (all upload endpoints)
- `/stream/**` (all streaming endpoints)

### Protected Endpoints
All other endpoints require a valid JWT token. Include the token in the request:
- **Header**: `Authorization: Bearer <token>`
- **Cookie**: JWT token in cookie (automatically extracted by gateway)

## Configuration

Swagger configuration is located in:
- **Config Class**: `com.Sparta.GatewayService.config.OpenApiConfig`
- **Properties**: `application.properties` (springdoc.* properties)

## Gateway Route Configuration

Routes are configured in `application.properties`:

```properties
# UserService route
spring.cloud.gateway.routes[0].id=userservice
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/account/**

# UploadService route
spring.cloud.gateway.routes[1].id=uploadservice
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/upload/**

# StreamingService route
spring.cloud.gateway.routes[2].id=streamingservice
spring.cloud.gateway.routes[2].uri=http://localhost:8083
spring.cloud.gateway.routes[2].predicates[0]=Path=/stream/**
```

## Customization

To customize the Swagger documentation, edit:
- `OpenApiConfig.java` - API info, servers, contact details
- Controller annotations - Add/modify `@Operation`, `@ApiResponses`, etc.
- Model annotations - Add/modify `@Schema` annotations

## Exporting Documentation

### Export OpenAPI Spec
- JSON: `http://localhost:8000/v3/api-docs`
- YAML: `http://localhost:8000/v3/api-docs.yaml`

You can use these specs with:
- Postman (import OpenAPI)
- Swagger Editor
- Other API documentation tools

## Troubleshooting

### Swagger UI not loading?
1. Check if the Gateway Service is running on port 8000
2. Verify SpringDoc dependency is in `pom.xml`
3. Check browser console for errors
4. Ensure downstream services (UserService, UploadService, StreamingService) are running

### Endpoints not showing?
1. Ensure controllers have `@RestController` annotation
2. Verify `@RequestMapping` annotations are correct
3. Check that routes are properly configured in `application.properties`

### 502 Bad Gateway errors?
1. Ensure downstream services are running:
   - UserService on port 8081
   - UploadService on port 8082
   - StreamingService on port 8083
2. Check service URLs in `application.properties`

### Authentication errors?
1. Verify JWT secret matches between GatewayService and UserService
2. Check token format: `Bearer <token>`
3. Ensure token is not expired

## Additional Resources

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)
- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
