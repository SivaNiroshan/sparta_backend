package com.sparta.UserService.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Forgot password request model")
public class ForgotPasswordRequest {
    @Schema(description = "User email address", required = true, example = "user@example.com")
    private String email;
}

