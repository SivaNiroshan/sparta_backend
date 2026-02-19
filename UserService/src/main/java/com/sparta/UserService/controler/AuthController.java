package com.sparta.UserService.controler;

import com.sparta.UserService.exception.ForgotException;
import com.sparta.UserService.exception.LoginException;
import com.sparta.UserService.exception.SignupException;
import com.sparta.UserService.model.ForgotPasswordRequest;
import com.sparta.UserService.model.LoginRequest;
import com.sparta.UserService.model.SignupRequest;
import com.sparta.UserService.model.VerifyOTPRequest;
import com.sparta.UserService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("account/auth")
@Tag(name = "Authentication", description = "API endpoints for user authentication, registration, and password management")
public class AuthController {

    @Autowired
    private  AuthService authService;


    @Operation(
            summary = "User login",
            description = "Authenticate user with email and password. Returns access token and refresh token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"refreshToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"userId\": \"123e4567-e89b-12d3-a456-426614174000\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Invalid email or password\"}")
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Map<String, Object> response = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (LoginException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @Operation(
            summary = "User registration",
            description = "Initiate user registration. Sends OTP to email for verification."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration initiated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"OTP sent to email\", \"email\": \"user@example.com\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already exists",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Email already registered\"}")
                    )
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request){
        try {
            Map<String, Object> response = authService.initiateSignup(
                request.getEmail(),
                request.getPassword(),
                request.getFirstname(),
                request.getLastname(),
                request.getUsername()
            );
            return ResponseEntity.ok(response);
        } catch (SignupException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }

    @Operation(
            summary = "Verify signup OTP",
            description = "Verify OTP sent during registration to complete user signup."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully, user account created",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Account created successfully\", \"userId\": \"123e4567-e89b-12d3-a456-426614174000\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Invalid or expired OTP\"}")
                    )
            )
    })
    @PostMapping("/verify-signup-otp")
    public ResponseEntity<?> verifySignupOTP(@RequestBody VerifyOTPRequest request){
        try {
            Map<String, Object> response = authService.verifySignupOTP(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(response);
        } catch (SignupException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @Operation(
            summary = "Check if email exists",
            description = "Check if an email address is already registered in the system."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email check completed",
                    content = @Content(
                            schema = @Schema(type = "boolean"),
                            examples = @ExampleObject(value = "true")
                    )
            )
    })
    @GetMapping("/email-exists")
    public boolean emailExists(
            @Parameter(description = "Email address to check", required = true, example = "user@example.com")
            @RequestParam String email) {
        return authService.emailExists(email);
    }

    @Operation(
            summary = "Update password",
            description = "Update user password by user ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password updated successfully",
                    content = @Content(
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(value = "Password updated successfully")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing userId or newPassword",
                    content = @Content(
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(value = "Missing userId or newPassword")
                    )
            )
    })
    @PatchMapping("/updatepassword")
    public ResponseEntity<String> changePassword(@RequestBody Map<String, String> request){
        String userId = request.get("userId");
        String newPassword = request.get("newPassword");

        if (userId == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Missing userId or newPassword");
        }

        return authService.updatePasswordByUID(UUID.fromString(userId),newPassword);
    }

    @Operation(
            summary = "Initiate password reset",
            description = "Request password reset. Sends OTP to email for verification."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset OTP sent successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"OTP sent to email\", \"email\": \"user@example.com\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Email not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Email not found\"}")
                    )
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            Map<String, Object> response = authService.initiateForgotPassword(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (ForgotException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @Operation(
            summary = "Verify forgot password OTP",
            description = "Verify OTP sent during password reset process."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"OTP verified\", \"resetToken\": \"token123\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Invalid or expired OTP\"}")
                    )
            )
    })
    @PostMapping("/verify-forgot-password-otp")
    public ResponseEntity<?> verifyForgotPasswordOTP(@RequestBody VerifyOTPRequest request) {
        try {
            Map<String, Object> response = authService.verifyForgotPasswordOTP(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(response);
        } catch (ForgotException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @Operation(
            summary = "Resend signup OTP",
            description = "Resend OTP for user registration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP resent successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"OTP resent to email\", \"email\": \"user@example.com\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"User not found or already verified\"}")
                    )
            )
    })
    @PostMapping("/resend-signup-otp")
    public ResponseEntity<?> resendSignupOTP(@RequestBody ForgotPasswordRequest request) {
        try {
            Map<String, Object> response = authService.resendSignupOTP(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (SignupException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @Operation(
            summary = "Resend forgot password OTP",
            description = "Resend OTP for password reset."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP resent successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"OTP resent to email\", \"email\": \"user@example.com\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Email not found\"}")
                    )
            )
    })
    @PostMapping("/resend-forgot-password-otp")
    public ResponseEntity<?> resendForgotPasswordOTP(@RequestBody ForgotPasswordRequest request) {
        try {
            Map<String, Object> response = authService.resendForgotPasswordOTP(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (ForgotException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generate new access token using refresh token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"refreshToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh token is required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Refresh token is required\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Invalid or expired refresh token\"}")
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Refresh token is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            Map<String, Object> response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (LoginException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @Operation(
            summary = "User logout",
            description = "Logout user and invalidate tokens. Tokens can be passed via request body or headers."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Logged out successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Logout failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Logout failed: Internal server error\"}")
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody(required = false) Map<String, String> request,
            @RequestHeader(value = "X-Access-Token", required = false) String accessTokenHeader,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader) {
        try {
            // Extract tokens from request body or headers (GatewayService passes via headers)
            String accessToken = null;
            String refreshToken = null;
            
            if (request != null) {
                accessToken = request.get("accessToken");
                refreshToken = request.get("refreshToken");
            }
            
            // Use headers if body doesn't have tokens (GatewayService passes via headers)
            if ((accessToken == null || accessToken.isEmpty()) && accessTokenHeader != null && !accessTokenHeader.isEmpty()) {
                accessToken = accessTokenHeader;
            }
            if ((refreshToken == null || refreshToken.isEmpty()) && refreshTokenHeader != null && !refreshTokenHeader.isEmpty()) {
                refreshToken = refreshTokenHeader;
            }
            
            Map<String, Object> response = authService.logout(accessToken, refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Logout failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}

