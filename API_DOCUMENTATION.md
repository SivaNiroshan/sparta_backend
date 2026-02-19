# UserService AuthController API Documentation

Base URL: `/account/auth`

All endpoints are prefixed with the base URL above.

---

## 1. Login

**Endpoint:** `POST /account/auth/login`

**Description:** Authenticates a user and returns a JWT token.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Success Response (200 OK):**
```json
{
  "user": {
    "id": "string (UUID)",
    "email": "string",
    "firstname": "string",
    "lastname": "string",
    "username": "string"
  },
  "message": "Login successful",
  "token": "string (JWT token)"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "Invalid email or password"
}
```

---

## 2. Signup

**Endpoint:** `POST /account/auth/signup`

**Description:** Initiates the signup process by sending an OTP to the user's email. The OTP must be verified using the verify-signup-otp endpoint.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "string",
  "password": "string",
  "firstname": "string",
  "lastname": "string",
  "username": "string"
}
```

**Success Response (200 OK):**
```json
{
  "message": "OTP sent to your email. Please verify within 5 minutes.",
  "email": "string"
}
```

**Error Response (409 Conflict):**
```json
{
  "error": "Email already exists"
}
```

---

## 3. Verify Signup OTP

**Endpoint:** `POST /account/auth/verify-signup-otp`

**Description:** Verifies the OTP sent during signup and completes the user registration process.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "string",
  "otp": "string"
}
```

**Success Response (200 OK):**
```json
{
  "user": {
    "id": "string (UUID)",
    "email": "string",
    "firstname": "string",
    "lastname": "string",
    "username": "string"
  },
  "message": "Signup successful",
  "token": "string (JWT token)"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Invalid OTP. Please try again."
}
```

or

```json
{
  "error": "OTP expired or invalid. Please request a new OTP."
}
```

---

## 4. Check Email Exists

**Endpoint:** `GET /account/auth/email-exists`

**Description:** Checks if an email address already exists in the system.

**Headers:**
```
None required
```

**Query Parameters:**
```
email: string (required)
```

**Example:** `GET /account/auth/email-exists?email=user@example.com`

**Success Response (200 OK):**
```
true or false (boolean)
```

---

## 5. Update Password

**Endpoint:** `PATCH /account/auth/updatepassword`

**Description:** Updates the password for a user by their user ID.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "userId": "string (UUID)",
  "newPassword": "string"
}
```

**Success Response (200 OK):**
```
"Password updated successfully"
```

**Error Response (400 Bad Request):**
```
"Missing userId or newPassword"
```

or

```
"User ID cannot be null"
```

**Error Response (404 Not Found):**
```
"User not found"
```

---

## 6. Forgot Password

**Endpoint:** `POST /account/auth/forgot-password`

**Description:** Initiates the password reset process by sending an OTP to the user's email.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "string"
}
```

**Success Response (200 OK):**
```json
{
  "message": "OTP sent to your email. Please verify within 5 minutes.",
  "email": "string"
}
```

**Error Response (404 Not Found):**
```json
{
  "error": "Email does not exist"
}
```

---

## 7. Verify Forgot Password OTP

**Endpoint:** `POST /account/auth/verify-forgot-password-otp`

**Description:** Verifies the OTP sent during the forgot password process and returns user details with a JWT token.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "string",
  "otp": "string"
}
```

**Success Response (200 OK):**
```json
{
  "user": {
    "id": "string (UUID)",
    "email": "string",
    "firstname": "string",
    "lastname": "string",
    "username": "string"
  },
  "message": "OTP verified successfully",
  "token": "string (JWT token)"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Invalid OTP. Please try again."
}
```

or

```json
{
  "error": "OTP expired or invalid. Please request a new OTP."
}
```

---

## 8. Resend Signup OTP

**Endpoint:** `POST /account/auth/resend-signup-otp`

**Description:** Resends a new OTP for the signup process. The previous OTP request must still be valid (within 5 minutes).

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "string"
}
```

**Success Response (200 OK):**
```json
{
  "message": "New OTP sent to your email. Please verify within 5 minutes.",
  "email": "string"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "No signup request found. Please start a new signup process."
}
```

or

```json
{
  "error": "Email already exists"
}
```

---

## 9. Resend Forgot Password OTP

**Endpoint:** `POST /account/auth/resend-forgot-password-otp`

**Description:** Resends a new OTP for the forgot password process. The previous password reset request must still be valid (within 5 minutes).

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "string"
}
```

**Success Response (200 OK):**
```json
{
  "message": "New OTP sent to your email. Please verify within 5 minutes.",
  "email": "string"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "No password reset request found. Please request a new password reset."
}
```

---

## Notes

- All OTPs expire after 5 minutes
- JWT tokens are returned in successful login, signup verification, and forgot password verification responses
- All endpoints that require JSON request bodies expect `Content-Type: application/json` header
- Error responses follow a consistent format with an "error" field containing the error message
- User IDs are UUIDs (Universally Unique Identifiers)

