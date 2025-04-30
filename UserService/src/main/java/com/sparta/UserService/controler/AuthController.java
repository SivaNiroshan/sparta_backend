package com.sparta.UserService.controler;

import com.sparta.UserService.model.LoginRequest;
import com.sparta.UserService.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    @Autowired
    private  AuthService authService;


    @PostMapping("/login")
    public Object login(@RequestBody LoginRequest request) {
        return authService.login(request.getEmail(), request.getPassword());
    }
}

