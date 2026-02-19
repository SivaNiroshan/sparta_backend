package com.sparta.UserService.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for API responses: friend/request/blocked user with userId and username.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FriendInfo {
    private UUID userId;
    private String username;
}
