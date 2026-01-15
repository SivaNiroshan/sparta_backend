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
 * MongoDB Document for storing user's friend relationships
 * Each document represents one user's complete friend list with:
 * - Current friends
 * - Blocked friends
 * - Sent friend requests
 * - Received friend requests
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
     * User ID from PostgreSQL RDS (UUID)
     * This is the owner of this friend list
     */
    private UUID userId;
    
    /**
     * List of current friend IDs (mutual friends)
     */
    private List<UUID> currentFriends;
    
    /**
     * List of blocked friend IDs
     */
    private List<UUID> blockedFriends;
    
    /**
     * List of friend IDs to whom friend requests have been sent (pending)
     */
    private List<UUID> sentRequests;
    
    /**
     * List of friend IDs from whom friend requests have been received (pending)
     */
    private List<UUID> receivedRequests;
    
    /**
     * Timestamp when the document was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the document was last updated
     */
    private LocalDateTime updatedAt;
    
    /**
     * Constructor for creating a new friend document for a user
     */
    public FriendDocument(UUID userId) {
        this.userId = userId;
        this.currentFriends = new ArrayList<>();
        this.blockedFriends = new ArrayList<>();
        this.sentRequests = new ArrayList<>();
        this.receivedRequests = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Helper method to update the timestamp
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}

