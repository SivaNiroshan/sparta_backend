package com.sparta.UserService.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Embedded entry for send/receive request arrays and blocked users.
 * Stores userId and username of the other user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestEntry {
    private UUID userId;
    private String username;
}
