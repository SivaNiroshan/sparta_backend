package com.sparta.UserService.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.UserService.exception.ForgotException;
import com.sparta.UserService.exception.LoginException;
import com.sparta.UserService.exception.SignupException;
import com.sparta.UserService.model.SignupData;
import com.sparta.UserService.model.UserDetails;
import com.sparta.UserService.repository.RegisterRepository;
import com.sparta.UserService.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

@Service
public class AuthService {

    @Autowired
    private RegisterRepository register;

    @Autowired
    private OTPService otpService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String REDIS_SIGNUP_PREFIX = "signup:otp:";
    private static final String REDIS_FORGOT_PASSWORD_PREFIX = "forgot:otp:";
    private static final int OTP_EXPIRATION_MINUTES = 5;

    public Map<String, Object> login(String email, String password) {
        UserDetails user = register.findByEmail(email);
        
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new LoginException("Invalid email or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("user", Map.of(
            "id", user.getId().toString(),
            "email", user.getEmail(),
            "firstname", user.getFirstname() != null ? user.getFirstname() : "",
            "lastname", user.getLastname() != null ? user.getLastname() : "",
            "username", user.getUsername() != null ? user.getUsername() : ""
        ));
        response.put("message", "Login successful");
        response.put("token", token);
        
        return response;
    }

    /**
     * Initiates signup process by sending OTP to user's email
     * Stores user data temporarily in Redis with 5-minute expiration
     */
    public Map<String, Object> initiateSignup(String email, String password, String firstname, String lastname, String username) {
        // Check if email already exists in database
        if (register.existsByEmail(email)) {
            throw new SignupException("Email already exists");
        }

        // Generate OTP
        String otp = otpService.sendOTPByEmail(email);

        // Hash password before storing in Redis
        String hashedPassword = passwordEncoder.encode(password);

        // Create SignupData object
        SignupData signupData = new SignupData();
        signupData.setEmail(email);
        signupData.setPassword(hashedPassword);
        signupData.setFirstname(firstname);
        signupData.setLastname(lastname);
        signupData.setUsername(username);
        signupData.setOtp(otp);
        signupData.setCreatedAt(System.currentTimeMillis());

        // Store in Redis with 5-minute expiration
        String redisKey = REDIS_SIGNUP_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, signupData, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Return response (do not include OTP)
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP sent to your email. Please verify within 5 minutes.");
        response.put("email", email);
        
        return response;
    }

    /**
     * Verifies OTP and completes signup by saving user to database
     */
    public Map<String, Object> verifySignupOTP(String email, String providedOTP) {
        String redisKey = REDIS_SIGNUP_PREFIX + email;
        
        // Retrieve signup data from Redis
        Object data = redisTemplate.opsForValue().get(redisKey);
        
        if (data == null) {
            throw new SignupException("OTP expired or invalid. Please request a new OTP.");
        }

        // Convert to SignupData object
        SignupData signupData = objectMapper.convertValue(data, SignupData.class);

        // Verify OTP
        if (!otpService.verifyOTP(providedOTP, signupData.getOtp())) {
            throw new SignupException("Invalid OTP. Please try again.");
        }

        // Check again if email exists (race condition protection)
        if (register.existsByEmail(email)) {
            // Delete Redis entry
            redisTemplate.delete(redisKey);
            throw new SignupException("Email already exists");
        }

        // Create and save user to database
        UserDetails details = new UserDetails();
        details.setEmail(signupData.getEmail());
        details.setPassword(signupData.getPassword()); // Already hashed
        details.setFirstname(signupData.getFirstname());
        details.setLastname(signupData.getLastname());
        details.setUsername(signupData.getUsername());

        // Save user - UUID will be auto-generated by JPA
        UserDetails savedUser = register.save(details);

        // Verify UUID was generated
        if (savedUser.getId() == null) {
            throw new SignupException("Failed to generate user ID. Please try again.");
        }

        // Delete Redis entry after successful signup
        redisTemplate.delete(redisKey);

        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail());

        // Return success response with generated UUID
        Map<String, Object> response = new HashMap<>();
        response.put("user", Map.of(
            "id", savedUser.getId().toString(),
            "email", savedUser.getEmail(),
            "firstname", savedUser.getFirstname() != null ? savedUser.getFirstname() : "",
            "lastname", savedUser.getLastname() != null ? savedUser.getLastname() : "",
            "username", savedUser.getUsername() != null ? savedUser.getUsername() : ""
        ));
        response.put("message", "Signup successful");
        response.put("token", token);
        
        return response;
    }

    /**
     * Legacy signup method - kept for backward compatibility if needed
     * @deprecated Use initiateSignup and verifySignupOTP instead
     */
    @Deprecated
    public Map<String, Object> signup(String email, String password, String firstname, String lastname, String username) {
        // Check if email already exists
        if (register.existsByEmail(email)) {
            throw new SignupException("Email already exists");
        }

        // Create new user
        UserDetails details = new UserDetails();
        details.setEmail(email);
        details.setPassword(passwordEncoder.encode(password));
        details.setFirstname(firstname);
        details.setLastname(lastname);
        details.setUsername(username);

        UserDetails savedUser = register.save(details);

        Map<String, Object> response = new HashMap<>();
        response.put("user", Map.of(
            "id", savedUser.getId().toString(),
            "email", savedUser.getEmail(),
            "firstname", savedUser.getFirstname() != null ? savedUser.getFirstname() : "",
            "lastname", savedUser.getLastname() != null ? savedUser.getLastname() : "",
            "username", savedUser.getUsername() != null ? savedUser.getUsername() : ""
        ));
        response.put("message", "Signup successful");
        
        return response;
    }

    public boolean emailExists(String email) {
        return register.existsByEmail(email);
    }

    public ResponseEntity<String> updatePasswordByUID(UUID userId, String newPassword) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User ID cannot be null");
        }
        
        Optional<UserDetails> userOptional = register.findById(userId);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserDetails user = userOptional.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        register.save(user);

        return ResponseEntity.ok("Password updated successfully");
    }

    /**
     * Initiates forgot password process by sending OTP to user's email
     * Checks if email exists, throws ForgotException if not
     * Stores user ID, email and OTP in Redis with 5-minute expiration
     */
    public Map<String, Object> initiateForgotPassword(String email) {
        // Check if email exists in database and get user
        UserDetails user = register.findByEmail(email);
        if (user == null) {
            throw new ForgotException("Email does not exist");
        }

        // Generate and send OTP
        String otp = otpService.sendOTPByEmail(email, "Password Reset OTP", 
            "You have requested to reset your password. Use the OTP below to verify your identity.");

        // Store user ID, email and OTP in Redis with 5-minute expiration
        String redisKey = REDIS_FORGOT_PASSWORD_PREFIX + email;
        Map<String, String> forgotPasswordData = new HashMap<>();
        forgotPasswordData.put("userId", user.getId().toString());
        forgotPasswordData.put("email", email);
        forgotPasswordData.put("otp", otp);
        forgotPasswordData.put("createdAt", String.valueOf(System.currentTimeMillis()));
        
        redisTemplate.opsForValue().set(redisKey, forgotPasswordData, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Return response (do not include OTP)
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP sent to your email. Please verify within 5 minutes.");
        response.put("email", email);
        
        return response;
    }

    /**
     * Verifies OTP for forgot password
     * If OTP matches within 5 minutes, fetches user details and returns firstname, lastname, username
     * If OTP doesn't match, throws exception (data stays in Redis until 5 min expiry)
     */
    public Map<String, Object> verifyForgotPasswordOTP(String email, String providedOTP) {
        String redisKey = REDIS_FORGOT_PASSWORD_PREFIX + email;
        
        // Retrieve forgot password data from Redis
        Object data = redisTemplate.opsForValue().get(redisKey);
        
        if (data == null) {
            throw new ForgotException("OTP expired or invalid. Please request a new OTP.");
        }

        // Convert to Map
        Map<String, String> forgotPasswordData = objectMapper.convertValue(data, new TypeReference<Map<String, String>>() {});

        // Verify OTP
        String storedOTP = forgotPasswordData.get("otp");
        if (!otpService.verifyOTP(providedOTP, storedOTP)) {
            // OTP doesn't match - throw exception but keep data in Redis until expiry
            throw new ForgotException("Invalid OTP. Please try again.");
        }

        // OTP matched - fetch user by ID
        String userIdStr = forgotPasswordData.get("userId");
        if (userIdStr == null || userIdStr.isEmpty()) {
            redisTemplate.delete(redisKey);
            throw new ForgotException("Invalid OTP data. Please request a new OTP.");
        }
        
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
            Objects.requireNonNull(userId, "User ID cannot be null");
        } catch (IllegalArgumentException e) {
            redisTemplate.delete(redisKey);
            throw new ForgotException("Invalid OTP data. Please request a new OTP.");
        }
        
        Optional<UserDetails> userOptional = register.findById(userId);
        
        if (userOptional.isEmpty()) {
            // User not found - delete Redis entry and throw exception
            redisTemplate.delete(redisKey);
            throw new ForgotException("User not found. Please request a new OTP.");
        }

        UserDetails user = userOptional.get();

        // Delete Redis entry after successful verification
        redisTemplate.delete(redisKey);

        // Return user details
        Map<String, Object> response = new HashMap<>();
        response.put("user", Map.of(
            "id", user.getId().toString(),
            "email", user.getEmail(),
            "firstname", user.getFirstname() != null ? user.getFirstname() : "",
            "lastname", user.getLastname() != null ? user.getLastname() : "",
            "username", user.getUsername() != null ? user.getUsername() : ""
        ));
        response.put("message", "OTP verified successfully");
        
        return response;
    }

    /**
     * Resends OTP for signup process
     * Updates OTP value and resets expiration to 5 minutes
     */
    public Map<String, Object> resendSignupOTP(String email) {
        String redisKey = REDIS_SIGNUP_PREFIX + email;
        
        // Retrieve existing signup data from Redis
        Object data = redisTemplate.opsForValue().get(redisKey);
        
        if (data == null) {
            throw new SignupException("No signup request found. Please start a new signup process.");
        }

        // Convert to SignupData object
        SignupData signupData = objectMapper.convertValue(data, SignupData.class);

        // Check again if email exists (race condition protection)
        if (register.existsByEmail(email)) {
            // Delete Redis entry
            redisTemplate.delete(redisKey);
            throw new SignupException("Email already exists");
        }

        // Generate new OTP
        String newOtp = otpService.sendOTPByEmail(email);

        // Update OTP and timestamp
        signupData.setOtp(newOtp);
        signupData.setCreatedAt(System.currentTimeMillis());

        // Update Redis with new OTP and reset 5-minute expiration
        redisTemplate.opsForValue().set(redisKey, signupData, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Return response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "New OTP sent to your email. Please verify within 5 minutes.");
        response.put("email", email);
        
        return response;
    }

    /**
     * Resends OTP for forgot password process
     * Updates OTP value and resets expiration to 5 minutes
     */
    public Map<String, Object> resendForgotPasswordOTP(String email) {
        String redisKey = REDIS_FORGOT_PASSWORD_PREFIX + email;
        
        // Retrieve existing forgot password data from Redis
        Object data = redisTemplate.opsForValue().get(redisKey);
        
        if (data == null) {
            throw new ForgotException("No password reset request found. Please request a new password reset.");
        }

        // Convert to Map
        Map<String, String> forgotPasswordData = objectMapper.convertValue(data, new TypeReference<Map<String, String>>() {});

        // Verify user still exists
        String userIdStr = forgotPasswordData.get("userId");
        if (userIdStr == null || userIdStr.isEmpty()) {
            redisTemplate.delete(redisKey);
            throw new ForgotException("Invalid reset request. Please request a new password reset.");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
            Objects.requireNonNull(userId, "User ID cannot be null");
        } catch (IllegalArgumentException e) {
            redisTemplate.delete(redisKey);
            throw new ForgotException("Invalid reset request. Please request a new password reset.");
        }

        Optional<UserDetails> userOptional = register.findById(userId);
        if (userOptional.isEmpty()) {
            redisTemplate.delete(redisKey);
            throw new ForgotException("User not found. Please request a new password reset.");
        }

        // Generate new OTP
        String newOtp = otpService.sendOTPByEmail(email, "Password Reset OTP", 
            "You have requested to reset your password. Use the OTP below to verify your identity.");

        // Update OTP and timestamp
        forgotPasswordData.put("otp", newOtp);
        forgotPasswordData.put("createdAt", String.valueOf(System.currentTimeMillis()));

        // Update Redis with new OTP and reset 5-minute expiration
        redisTemplate.opsForValue().set(redisKey, forgotPasswordData, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Return response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "New OTP sent to your email. Please verify within 5 minutes.");
        response.put("email", email);
        
        return response;
    }
}
