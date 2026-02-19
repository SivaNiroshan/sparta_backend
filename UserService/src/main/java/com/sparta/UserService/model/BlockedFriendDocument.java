package com.sparta.UserService.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB Document for the blocked_friends collection.
 * One document per user: userId, blockedUsers array (userId, username).
 */
@Document(collection = "blocked_friends")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlockedFriendDocument {

    @Id
    private String id;

    private UUID userId;

    /**
     * Users blocked by this user (each entry: userId, username).
     */
    private List<FriendRequestEntry> blockedUsers = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BlockedFriendDocument(UUID userId) {
        this.userId = userId;
        this.blockedUsers = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
