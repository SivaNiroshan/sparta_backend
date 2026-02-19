package com.sparta.UserService.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Login request model")
public class LoginRequest {
    @Schema(description = "User email address", required = true, example = "user@example.com")
    private String email;
    
    @Schema(description = "User password", required = true, example = "SecurePassword123!")
    private String password;
}
