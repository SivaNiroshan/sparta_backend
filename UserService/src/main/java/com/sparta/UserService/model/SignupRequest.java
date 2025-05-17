package com.sparta.UserService.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String firstname;
    private String lastname;
    private String username;
    private String email;
    private String password;

}
