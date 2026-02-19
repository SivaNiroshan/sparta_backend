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
 * MongoDB Document for the friends collection.
 * One document per user: id, userId, friends array (friendId, friendUsername).
 */
@Document(collection = "friends")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FriendDocument {

    @Id
    private String id;

    /**
     * Owner of this friend list (user ID from PostgreSQL RDS).
     */
    private UUID userId;

    /**
     * List of current friends: each entry has friendId and friendUsername.
     */
    private List<FriendEntry> friends = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FriendDocument(UUID userId) {
        this.userId = userId;
        this.friends = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
