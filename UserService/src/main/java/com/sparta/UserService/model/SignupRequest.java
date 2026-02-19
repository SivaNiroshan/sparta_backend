package com.sparta.UserService.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "User registration request model")
public class SignupRequest {
    @Schema(description = "User first name", required = true, example = "John")
    private String firstname;
    
    @Schema(description = "User last name", required = true, example = "Doe")
    private String lastname;
    
    @Schema(description = "Username", required = true, example = "johndoe")
    private String username;
    
    @Schema(description = "User email address", required = true, example = "user@example.com")
    private String email;
    
    @Schema(description = "User password", required = true, example = "SecurePassword123!")
    private String password;
}
