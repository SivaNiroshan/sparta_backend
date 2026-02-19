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
 * MongoDB Document for the friend_requests collection.
 * One document per user: userId, sendRequest array, receiveRequest array.
 * Each array contains objects with userId and username.
 */
@Document(collection = "friend_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDocument {

    @Id
    private String id;

    private UUID userId;

    /**
     * Requests this user has sent (each entry: userId, username of the recipient).
     */
    private List<FriendRequestEntry> sendRequest = new ArrayList<>();

    /**
     * Requests this user has received (each entry: userId, username of the sender).
     */
    private List<FriendRequestEntry> receiveRequest = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FriendRequestDocument(UUID userId) {
        this.userId = userId;
        this.sendRequest = new ArrayList<>();
        this.receiveRequest = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
