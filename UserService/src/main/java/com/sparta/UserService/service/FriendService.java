package com.sparta.UserService.service;

import com.sparta.UserService.model.FriendDocument;
import com.sparta.UserService.model.UserDetails;
import com.sparta.UserService.repository.FriendMongoRepository;
import com.sparta.UserService.repository.RegisterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Friend Service using MongoDB for friend relationships
 * Integrates with PostgreSQL RDS for user validation
 */
@Service
public class FriendService {

    @Autowired
    private FriendMongoRepository friendMongoRepository;

    @Autowired
    private RegisterRepository registerRepository;

    /**
     * Get or create friend document for a user
     */
    private FriendDocument getOrCreateFriendDocument(UUID userId) {
        return friendMongoRepository.findByUserId(userId)
                .orElseGet(() -> {
                    FriendDocument newDoc = new FriendDocument(userId);
                    return friendMongoRepository.save(newDoc);
                });
    }

    /**
     * Add a new friend
     * Validates friend ID exists in PostgreSQL RDS before adding
     * 
     * @param userId Current user ID
     * @param friendId Friend ID to add (must exist in PostgreSQL)
     * @return Response with success/error message
     */
    public ResponseEntity<?> addFriend(UUID userId, UUID friendId) {
        try {
            // Validate user exists in PostgreSQL
            if (!registerRepository.existsById(userId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found in database");
            }

            // Validate friend exists in PostgreSQL RDS
            Optional<UserDetails> friendOptional = registerRepository.findById(friendId);
            if (friendOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Friend ID not found in database");
            }

            // Cannot add self as friend
            if (userId.equals(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot add yourself as a friend");
            }

            // Get or create friend document for current user
            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);
            
            // Check if already a friend
            if (userFriendDoc.getCurrentFriends().contains(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User is already your friend");
            }

            // Check if blocked
            if (userFriendDoc.getBlockedFriends().contains(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot add blocked user as friend. Unblock first.");
            }

            // Check if there's a pending request from this friend
            if (userFriendDoc.getReceivedRequests().contains(friendId)) {
                // Accept the request - make them friends
                userFriendDoc.getReceivedRequests().remove(friendId);
                userFriendDoc.getCurrentFriends().add(friendId);
                userFriendDoc.updateTimestamp();
                friendMongoRepository.save(userFriendDoc);

                // Update the friend's document - remove from sent requests, add to current friends
                FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
                friendDoc.getSentRequests().remove(userId);
                if (!friendDoc.getCurrentFriends().contains(userId)) {
                    friendDoc.getCurrentFriends().add(userId);
                }
                friendDoc.updateTimestamp();
                friendMongoRepository.save(friendDoc);

                return ResponseEntity.ok(Map.of(
                    "message", "Friend request accepted. You are now friends!",
                    "friendId", friendId.toString()
                ));
            }

            // Check if already sent a request
            if (userFriendDoc.getSentRequests().contains(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Friend request already sent");
            }

            // Add to sent requests
            userFriendDoc.getSentRequests().add(friendId);
            userFriendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);

            // Add to friend's received requests
            FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
            if (!friendDoc.getReceivedRequests().contains(userId)) {
                friendDoc.getReceivedRequests().add(userId);
                friendDoc.updateTimestamp();
                friendMongoRepository.save(friendDoc);
            }

            return ResponseEntity.ok(Map.of(
                "message", "Friend request sent successfully",
                "friendId", friendId.toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Get all current friends for a user
     * Returns user details from PostgreSQL RDS
     */
    public ResponseEntity<?> getCurrentFriends(UUID userId) {
        try {
            Optional<FriendDocument> friendDocOpt = friendMongoRepository.findByUserId(userId);
            
            if (friendDocOpt.isEmpty() || friendDocOpt.get().getCurrentFriends().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            FriendDocument friendDoc = friendDocOpt.get();
            List<UUID> friendIds = friendDoc.getCurrentFriends();

            // Fetch user details from PostgreSQL
            List<UserDetails> friends = friendIds.stream()
                    .map(id -> registerRepository.findById(id))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(friends);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Get blocked friends for a user
     */
    public ResponseEntity<?> getBlockedFriends(UUID userId) {
        try {
            Optional<FriendDocument> friendDocOpt = friendMongoRepository.findByUserId(userId);
            
            if (friendDocOpt.isEmpty() || friendDocOpt.get().getBlockedFriends().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            FriendDocument friendDoc = friendDocOpt.get();
            List<UUID> blockedIds = friendDoc.getBlockedFriends();

            // Fetch user details from PostgreSQL
            List<UserDetails> blockedFriends = blockedIds.stream()
                    .map(id -> registerRepository.findById(id))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(blockedFriends);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Get sent friend requests (pending)
     */
    public ResponseEntity<?> getSentRequests(UUID userId) {
        try {
            Optional<FriendDocument> friendDocOpt = friendMongoRepository.findByUserId(userId);
            
            if (friendDocOpt.isEmpty() || friendDocOpt.get().getSentRequests().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            FriendDocument friendDoc = friendDocOpt.get();
            List<UUID> sentRequestIds = friendDoc.getSentRequests();

            // Fetch user details from PostgreSQL
            List<UserDetails> sentRequests = sentRequestIds.stream()
                    .map(id -> registerRepository.findById(id))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(sentRequests);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Get received friend requests (pending)
     */
    public ResponseEntity<?> getReceivedRequests(UUID userId) {
        try {
            Optional<FriendDocument> friendDocOpt = friendMongoRepository.findByUserId(userId);
            
            if (friendDocOpt.isEmpty() || friendDocOpt.get().getReceivedRequests().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            FriendDocument friendDoc = friendDocOpt.get();
            List<UUID> receivedRequestIds = friendDoc.getReceivedRequests();

            // Fetch user details from PostgreSQL
            List<UserDetails> receivedRequests = receivedRequestIds.stream()
                    .map(id -> registerRepository.findById(id))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(receivedRequests);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Accept a friend request
     */
    public ResponseEntity<?> acceptFriendRequest(UUID userId, UUID friendId) {
        try {
            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);

            if (!userFriendDoc.getReceivedRequests().contains(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No pending friend request from this user");
            }

            // Remove from received requests and add to current friends
            userFriendDoc.getReceivedRequests().remove(friendId);
            if (!userFriendDoc.getCurrentFriends().contains(friendId)) {
                userFriendDoc.getCurrentFriends().add(friendId);
            }
            userFriendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);

            // Update friend's document
            FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
            friendDoc.getSentRequests().remove(userId);
            if (!friendDoc.getCurrentFriends().contains(userId)) {
                friendDoc.getCurrentFriends().add(userId);
            }
            friendDoc.updateTimestamp();
            friendMongoRepository.save(friendDoc);

            return ResponseEntity.ok(Map.of(
                "message", "Friend request accepted",
                "friendId", friendId.toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Reject a friend request
     */
    public ResponseEntity<?> rejectFriendRequest(UUID userId, UUID friendId) {
        try {
            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);

            if (!userFriendDoc.getReceivedRequests().contains(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No pending friend request from this user");
            }

            // Remove from received requests
            userFriendDoc.getReceivedRequests().remove(friendId);
            userFriendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);

            // Remove from friend's sent requests
            FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
            friendDoc.getSentRequests().remove(userId);
            friendDoc.updateTimestamp();
            friendMongoRepository.save(friendDoc);

            return ResponseEntity.ok(Map.of(
                "message", "Friend request rejected",
                "friendId", friendId.toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Block a friend
     */
    public ResponseEntity<?> blockFriend(UUID userId, UUID friendId) {
        try {
            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);

            if (userFriendDoc.getBlockedFriends().contains(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User is already blocked");
            }

            // Remove from current friends if exists
            userFriendDoc.getCurrentFriends().remove(friendId);
            
            // Remove from sent/received requests
            userFriendDoc.getSentRequests().remove(friendId);
            userFriendDoc.getReceivedRequests().remove(friendId);
            
            // Add to blocked friends
            if (!userFriendDoc.getBlockedFriends().contains(friendId)) {
                userFriendDoc.getBlockedFriends().add(friendId);
            }
            
            userFriendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);

            // Also remove from friend's current friends and requests
            FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
            friendDoc.getCurrentFriends().remove(userId);
            friendDoc.getSentRequests().remove(userId);
            friendDoc.getReceivedRequests().remove(userId);
            friendDoc.updateTimestamp();
            friendMongoRepository.save(friendDoc);

            return ResponseEntity.ok(Map.of(
                "message", "User blocked successfully",
                "friendId", friendId.toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Unblock a friend
     */
    public ResponseEntity<?> unblockFriend(UUID userId, UUID friendId) {
        try {
            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);

            if (!userFriendDoc.getBlockedFriends().contains(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User is not blocked");
            }

            // Remove from blocked friends
            userFriendDoc.getBlockedFriends().remove(friendId);
            userFriendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);

            return ResponseEntity.ok(Map.of(
                "message", "User unblocked successfully",
                "friendId", friendId.toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Remove a friend (unfriend)
     */
    public ResponseEntity<?> removeFriend(UUID userId, UUID friendId) {
        try {
            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);

            if (!userFriendDoc.getCurrentFriends().contains(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User is not your friend");
            }

            // Remove from current friends
            userFriendDoc.getCurrentFriends().remove(friendId);
            userFriendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);

            // Also remove from friend's current friends
            FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
            friendDoc.getCurrentFriends().remove(userId);
            friendDoc.updateTimestamp();
            friendMongoRepository.save(friendDoc);

            return ResponseEntity.ok(Map.of(
                "message", "Friend removed successfully",
                "friendId", friendId.toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Search friends by username, email, firstname, or lastname
     * Searches in current friends list
     */
    public ResponseEntity<?> searchFriends(UUID userId, String searchQuery) {
        try {
            // Get current friends
            Optional<FriendDocument> friendDocOpt = friendMongoRepository.findByUserId(userId);
            
            if (friendDocOpt.isEmpty() || friendDocOpt.get().getCurrentFriends().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            FriendDocument friendDoc = friendDocOpt.get();
            List<UUID> friendIds = friendDoc.getCurrentFriends();

            // Fetch all friends from PostgreSQL
            List<UserDetails> allFriends = friendIds.stream()
                    .map(id -> registerRepository.findById(id))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            // Filter by search query (case-insensitive)
            String query = searchQuery.toLowerCase();
            List<UserDetails> filteredFriends = allFriends.stream()
                    .filter(friend -> 
                        (friend.getUsername() != null && friend.getUsername().toLowerCase().contains(query)) ||
                        (friend.getEmail() != null && friend.getEmail().toLowerCase().contains(query)) ||
                        (friend.getFirstname() != null && friend.getFirstname().toLowerCase().contains(query)) ||
                        (friend.getLastname() != null && friend.getLastname().toLowerCase().contains(query))
                    )
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filteredFriends);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    /**
     * Cancel a sent friend request
     */
    public ResponseEntity<?> cancelSentRequest(UUID userId, UUID friendId) {
        try {
            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);

            if (!userFriendDoc.getSentRequests().contains(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No pending sent request to this user");
            }

            // Remove from sent requests
            userFriendDoc.getSentRequests().remove(friendId);
            userFriendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);

            // Remove from friend's received requests
            FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
            friendDoc.getReceivedRequests().remove(userId);
            friendDoc.updateTimestamp();
            friendMongoRepository.save(friendDoc);

            return ResponseEntity.ok(Map.of(
                "message", "Friend request cancelled",
                "friendId", friendId.toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }
}
