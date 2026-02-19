package com.sparta.UserService.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Embedded entry for the friends array in FriendDocument.
 * Stores friendId and friendUsername.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FriendEntry {
    private UUID friendId;
    private String friendUsername;
}
