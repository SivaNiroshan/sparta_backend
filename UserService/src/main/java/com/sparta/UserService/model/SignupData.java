package com.sparta.UserService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignupData implements Serializable {
    private String firstname;
    private String lastname;
    private String username;
    private String email;
    private String password; // Already hashed
    private String otp;
    private long createdAt;
}

