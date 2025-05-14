package com.sparta.UserService.controler;

import com.sparta.UserService.model.LoginRequest;
import com.sparta.UserService.model.SignupRequest;
import com.sparta.UserService.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("account/auth")
public class AuthController {

    @Autowired
    private  AuthService authService;


    @PostMapping("/login")
    public Object login(@RequestBody LoginRequest request) {
        return authService.login(request.getEmail(), request.getPassword());
    }

    @PostMapping("/signup")
    public Object signup(@RequestBody SignupRequest request){
        return authService.signup(request.getEmail(),request.getPassword(),request.getFirstname(),request.getLastname(),request.getUsername());

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



}

