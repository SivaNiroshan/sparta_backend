# Swagger API Documentation Guide

## Overview
This UserService uses **SpringDoc OpenAPI** (Swagger 3) for API documentation. The documentation is automatically generated from the code annotations and provides an interactive UI for testing APIs.

## Accessing Swagger UI

Once the application is running, you can access the Swagger UI at:

**Local Development:**
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- OpenAPI YAML: `http://localhost:8081/v3/api-docs.yaml`

## Features

### 1. Interactive API Testing
- Test all endpoints directly from the browser
- View request/response examples
- See all available parameters and their descriptions

### 2. Complete API Documentation
- All endpoints are documented with:
  - Request methods (GET, POST, PATCH, DELETE)
  - Request paths and parameters
  - Request body schemas
  - Response status codes
  - Response body examples
  - Error responses

### 3. Model Documentation
- All request/response models are documented with:
  - Field descriptions
  - Required fields
  - Example values
  - Data types

## API Endpoints Documented

### Authentication Endpoints (`/account/auth`)
- `POST /account/auth/login` - User login
- `POST /account/auth/signup` - User registration
- `POST /account/auth/verify-signup-otp` - Verify signup OTP
- `GET /account/auth/email-exists` - Check if email exists
- `PATCH /account/auth/updatepassword` - Update password
- `POST /account/auth/forgot-password` - Initiate password reset
- `POST /account/auth/verify-forgot-password-otp` - Verify password reset OTP
- `POST /account/auth/resend-signup-otp` - Resend signup OTP
- `POST /account/auth/resend-forgot-password-otp` - Resend password reset OTP
- `POST /account/auth/refresh` - Refresh access token
- `POST /account/auth/logout` - User logout

### Friend Management Endpoints (`/account/friend`)
- `POST /account/friend/add` - Add friend or send friend request
- `GET /account/friend/current` - Get current friends
- `GET /account/friend/blocked` - Get blocked friends
- `GET /account/friend/sent-requests` - Get sent friend requests
- `GET /account/friend/received-requests` - Get received friend requests
- `POST /account/friend/accept` - Accept friend request
- `POST /account/friend/reject` - Reject friend request
- `POST /account/friend/cancel-request` - Cancel sent friend request
- `POST /account/friend/block` - Block a friend
- `POST /account/friend/unblock` - Unblock a user
- `DELETE /account/friend/remove` - Remove a friend
- `GET /account/friend/search` - Search friends
- `GET /account/friend/get` - Get friends (Legacy endpoint)

## Using Swagger UI

1. **Navigate to Swagger UI**: Open `http://localhost:8081/swagger-ui.html` in your browser

2. **Explore APIs**: 
   - APIs are grouped by tags (Authentication, Friend Management)
   - Click on any endpoint to expand and see details

3. **Test an Endpoint**:
   - Click "Try it out" button
   - Fill in the required parameters
   - Click "Execute"
   - View the response below

4. **View Models**: 
   - Scroll down to see all request/response models
   - Click on any model to see its schema

## Configuration

Swagger configuration is located in:
- **Config Class**: `com.sparta.UserService.config.OpenApiConfig`
- **Properties**: `application.properties` (springdoc.* properties)

## Customization

To customize the Swagger documentation, edit:
- `OpenApiConfig.java` - API info, servers, contact details
- Controller annotations - Add/modify `@Operation`, `@ApiResponses`, etc.
- Model annotations - Add/modify `@Schema` annotations

## Exporting Documentation

### Export OpenAPI Spec
- JSON: `http://localhost:8081/v3/api-docs`
- YAML: `http://localhost:8081/v3/api-docs.yaml`

You can use these specs with:
- Postman (import OpenAPI)
- Swagger Editor
- Other API documentation tools

## Troubleshooting

### Swagger UI not loading?
1. Check if the application is running on port 8081
2. Verify SpringDoc dependency is in `pom.xml`
3. Check browser console for errors

### Endpoints not showing?
1. Ensure controllers have `@RestController` annotation
2. Verify `@RequestMapping` annotations are correct
3. Check SecurityConfig allows Swagger paths

### Models not showing?
1. Ensure model classes have `@Schema` annotations
2. Check if models are used in controller methods
3. Verify Lombok annotations are present

## Additional Resources

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)
