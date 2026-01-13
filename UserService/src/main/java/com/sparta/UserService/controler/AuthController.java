package com.sparta.UserService.controler;

import com.sparta.UserService.exception.ForgotException;
import com.sparta.UserService.exception.LoginException;
import com.sparta.UserService.exception.SignupException;
import com.sparta.UserService.model.ForgotPasswordRequest;
import com.sparta.UserService.model.LoginRequest;
import com.sparta.UserService.model.SignupRequest;
import com.sparta.UserService.model.VerifyOTPRequest;
import com.sparta.UserService.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("account/auth")
public class AuthController {

    @Autowired
    private  AuthService authService;


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

    @GetMapping("/email-exists")
    public boolean emailExists(@RequestParam String email) {
        return authService.emailExists(email);
    }

    @PatchMapping("/updatepassword")
    public ResponseEntity<String> changePassword(@RequestBody Map<String, String> request){
        String userId = request.get("userId");
        String newPassword = request.get("newPassword");

        if (userId == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Missing userId or newPassword");
        }

        return authService.updatePasswordByUID(UUID.fromString(userId),newPassword);
    }

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

}

