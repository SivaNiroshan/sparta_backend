package com.sparta.UserService.repository;

import com.sparta.UserService.model.FriendDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MongoDB Repository for FriendDocument
 * Handles all database operations for friend relationships
 */
@Repository
public interface FriendMongoRepository extends MongoRepository<FriendDocument, String> {
    
    /**
     * Find friend document by user ID
     */
    Optional<FriendDocument> findByUserId(UUID userId);
    
    /**
     * Check if a friend document exists for a user
     */
    boolean existsByUserId(UUID userId);
    
    /**
     * Find all users who have the given userId in their current friends list
     * This helps in bidirectional friend relationships
     */
    @Query("{ 'currentFriends': ?0 }")
    List<FriendDocument> findUsersWhoHaveAsFriend(UUID friendId);
    
    /**
     * Find all users who have the given userId in their sent requests
     * This helps in finding mutual friend requests
     */
    @Query("{ 'sentRequests': ?0 }")
    List<FriendDocument> findUsersWhoSentRequestTo(UUID userId);
    
    /**
     * Find all users who have the given userId in their received requests
     */
    @Query("{ 'receivedRequests': ?0 }")
    List<FriendDocument> findUsersWhoReceivedRequestFrom(UUID userId);
    
    /**
     * Find all users who have the given userId in their blocked friends list
     */
    @Query("{ 'blockedFriends': ?0 }")
    List<FriendDocument> findUsersWhoBlocked(UUID userId);
}

