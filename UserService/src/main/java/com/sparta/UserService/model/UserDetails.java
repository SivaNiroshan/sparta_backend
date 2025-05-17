package com.sparta.UserService.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
@Entity
public class UserDetails {
    @Id
    private UUID id;
    private String firstname;
    private String lastname;
    private String username;
}
