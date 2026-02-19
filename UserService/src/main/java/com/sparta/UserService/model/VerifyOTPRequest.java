package com.sparta.UserService.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "OTP verification request model")
public class VerifyOTPRequest {
    @Schema(description = "User email address", required = true, example = "user@example.com")
    private String email;
    
    @Schema(description = "One-time password (OTP) code", required = true, example = "123456")
    private String otp;
}

