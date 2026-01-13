package com.sparta.UserService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.UserService.exception.LoginException;
import com.sparta.UserService.exception.SignupException;
import com.sparta.UserService.model.SignupData;
import com.sparta.UserService.model.UserDetails;
import com.sparta.UserService.repository.RegisterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

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

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String REDIS_SIGNUP_PREFIX = "signup:otp:";
    private static final int OTP_EXPIRATION_MINUTES = 5;

    public Map<String, Object> login(String email, String password) {
        UserDetails user = register.findByEmail(email);
        
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new LoginException("Invalid email or password");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("user", Map.of(
            "id", user.getId().toString(),
            "email", user.getEmail(),
            "firstname", user.getFirstname() != null ? user.getFirstname() : "",
            "lastname", user.getLastname() != null ? user.getLastname() : "",
            "username", user.getUsername() != null ? user.getUsername() : ""
        ));
        response.put("message", "Login successful");
        
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

        UserDetails savedUser = register.save(details);

        // Delete Redis entry after successful signup
        redisTemplate.delete(redisKey);

        // Return success response
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
        Optional<UserDetails> userOptional = register.findById(userId);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserDetails user = userOptional.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        register.save(user);

        return ResponseEntity.ok("Password updated successfully");
    }
}
