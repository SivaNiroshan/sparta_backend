package com.sparta.UserService.service;

import com.sparta.UserService.model.*;
import com.sparta.UserService.repository.BlockedFriendMongoRepository;
import com.sparta.UserService.repository.FriendMongoRepository;
import com.sparta.UserService.repository.FriendRequestMongoRepository;
import com.sparta.UserService.repository.RegisterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Friend Service using three MongoDB collections: friends, friend_requests, blocked_friends.
 * Integrates with PostgreSQL RDS for user validation and usernames.
 */
@Service
public class FriendService {

    @Autowired
    private FriendMongoRepository friendMongoRepository;

    @Autowired
    private FriendRequestMongoRepository friendRequestMongoRepository;

    @Autowired
    private BlockedFriendMongoRepository blockedFriendMongoRepository;

    @Autowired
    private RegisterRepository registerRepository;

    private FriendDocument getOrCreateFriendDocument(UUID userId) {
        return friendMongoRepository.findByUserId(userId)
                .orElseGet(() -> {
                    FriendDocument doc = new FriendDocument(userId);
                    return friendMongoRepository.save(doc);
                });
    }

    private FriendRequestDocument getOrCreateFriendRequestDocument(UUID userId) {
        return friendRequestMongoRepository.findByUserId(userId)
                .orElseGet(() -> {
                    FriendRequestDocument doc = new FriendRequestDocument(userId);
                    return friendRequestMongoRepository.save(doc);
                });
    }

    private BlockedFriendDocument getOrCreateBlockedDocument(UUID userId) {
        return blockedFriendMongoRepository.findByUserId(userId)
                .orElseGet(() -> {
                    BlockedFriendDocument doc = new BlockedFriendDocument(userId);
                    return blockedFriendMongoRepository.save(doc);
                });
    }

    private static FriendInfo toFriendInfo(FriendEntry e) {
        return new FriendInfo(e.getFriendId(), e.getFriendUsername());
    }

    private static FriendInfo toFriendInfo(FriendRequestEntry e) {
        return new FriendInfo(e.getUserId(), e.getUsername());
    }

    private boolean isBlocked(UUID userId, UUID targetId) {
        return blockedFriendMongoRepository.findByUserId(userId)
                .map(doc -> doc.getBlockedUsers().stream().anyMatch(e -> e.getUserId().equals(targetId)))
                .orElse(false);
    }

    private boolean hasInReceived(UUID userId, UUID fromId) {
        return friendRequestMongoRepository.findByUserId(userId)
                .map(doc -> doc.getReceiveRequest().stream().anyMatch(e -> e.getUserId().equals(fromId)))
                .orElse(false);
    }

    private boolean hasInSent(UUID userId, UUID toId) {
        return friendRequestMongoRepository.findByUserId(userId)
                .map(doc -> doc.getSendRequest().stream().anyMatch(e -> e.getUserId().equals(toId)))
                .orElse(false);
    }

    private boolean isCurrentFriend(UUID userId, UUID friendId) {
        return friendMongoRepository.findByUserId(userId)
                .map(doc -> doc.getFriends().stream().anyMatch(e -> e.getFriendId().equals(friendId)))
                .orElse(false);
    }

    public ResponseEntity<?> addFriend(UUID userId, UUID friendId) {
        try {
            if (!registerRepository.existsById(userId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found in database");
            }
            Optional<UserDetails> friendOpt = registerRepository.findById(friendId);
            if (friendOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Friend ID not found in database");
            }
            if (userId.equals(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot add yourself as a friend");
            }

            UserDetails friendUser = friendOpt.get();
            String friendUsername = friendUser.getUsername() != null ? friendUser.getUsername() : "";

            if (isBlocked(userId, friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot add blocked user as friend. Unblock first.");
            }
            if (isCurrentFriend(userId, friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is already your friend");
            }

            if (hasInReceived(userId, friendId)) {
                // Accept: add to friends in both docs, remove from request docs
                Optional<UserDetails> currentUserOpt = registerRepository.findById(userId);
                String currentUsername = currentUserOpt.map(u -> u.getUsername() != null ? u.getUsername() : "").orElse("");

                FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);
                FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
                userFriendDoc.getFriends().add(new FriendEntry(friendId, friendUsername));
                friendDoc.getFriends().add(new FriendEntry(userId, currentUsername));
                userFriendDoc.updateTimestamp();
                friendDoc.updateTimestamp();
                friendMongoRepository.save(userFriendDoc);
                friendMongoRepository.save(friendDoc);

                FriendRequestDocument userReq = getOrCreateFriendRequestDocument(userId);
                FriendRequestDocument friendReq = getOrCreateFriendRequestDocument(friendId);
                userReq.getReceiveRequest().removeIf(e -> e.getUserId().equals(friendId));
                friendReq.getSendRequest().removeIf(e -> e.getUserId().equals(userId));
                userReq.updateTimestamp();
                friendReq.updateTimestamp();
                friendRequestMongoRepository.save(userReq);
                friendRequestMongoRepository.save(friendReq);

                return ResponseEntity.ok(Map.of(
                        "message", "Friend request accepted. You are now friends!",
                        "friendId", friendId.toString()
                ));
            }

            if (hasInSent(userId, friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Friend request already sent");
            }

            Optional<UserDetails> currentUserOpt = registerRepository.findById(userId);
            String currentUsername = currentUserOpt.map(u -> u.getUsername() != null ? u.getUsername() : "").orElse("");

            FriendRequestDocument userReq = getOrCreateFriendRequestDocument(userId);
            FriendRequestDocument friendReq = getOrCreateFriendRequestDocument(friendId);
            userReq.getSendRequest().add(new FriendRequestEntry(friendId, friendUsername));
            friendReq.getReceiveRequest().add(new FriendRequestEntry(userId, currentUsername));
            userReq.updateTimestamp();
            friendReq.updateTimestamp();
            friendRequestMongoRepository.save(userReq);
            friendRequestMongoRepository.save(friendReq);

            return ResponseEntity.ok(Map.of(
                    "message", "Friend request sent successfully",
                    "friendId", friendId.toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getCurrentFriends(UUID userId) {
        try {
            Optional<FriendDocument> opt = friendMongoRepository.findByUserId(userId);
            if (opt.isEmpty() || opt.get().getFriends().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<FriendInfo> list = opt.get().getFriends().stream()
                    .map(FriendService::toFriendInfo)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getBlockedFriends(UUID userId) {
        try {
            Optional<BlockedFriendDocument> opt = blockedFriendMongoRepository.findByUserId(userId);
            if (opt.isEmpty() || opt.get().getBlockedUsers().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<FriendInfo> list = opt.get().getBlockedUsers().stream()
                    .map(FriendService::toFriendInfo)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getSentRequests(UUID userId) {
        try {
            Optional<FriendRequestDocument> opt = friendRequestMongoRepository.findByUserId(userId);
            if (opt.isEmpty() || opt.get().getSendRequest().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<FriendInfo> list = opt.get().getSendRequest().stream()
                    .map(FriendService::toFriendInfo)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getReceivedRequests(UUID userId) {
        try {
            Optional<FriendRequestDocument> opt = friendRequestMongoRepository.findByUserId(userId);
            if (opt.isEmpty() || opt.get().getReceiveRequest().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<FriendInfo> list = opt.get().getReceiveRequest().stream()
                    .map(FriendService::toFriendInfo)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> acceptFriendRequest(UUID userId, UUID friendId) {
        try {
            FriendRequestDocument userReq = getOrCreateFriendRequestDocument(userId);
            boolean inReceived = userReq.getReceiveRequest().stream().anyMatch(e -> e.getUserId().equals(friendId));
            if (!inReceived) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No pending friend request from this user");
            }

            Optional<UserDetails> friendOpt = registerRepository.findById(friendId);
            String friendUsername = friendOpt.map(u -> u.getUsername() != null ? u.getUsername() : "").orElse("");
            Optional<UserDetails> userOpt = registerRepository.findById(userId);
            String userUsername = userOpt.map(u -> u.getUsername() != null ? u.getUsername() : "").orElse("");

            userReq.getReceiveRequest().removeIf(e -> e.getUserId().equals(friendId));
            userReq.updateTimestamp();
            friendRequestMongoRepository.save(userReq);

            FriendRequestDocument friendReq = getOrCreateFriendRequestDocument(friendId);
            friendReq.getSendRequest().removeIf(e -> e.getUserId().equals(userId));
            friendReq.updateTimestamp();
            friendRequestMongoRepository.save(friendReq);

            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);
            FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
            if (userFriendDoc.getFriends().stream().noneMatch(e -> e.getFriendId().equals(friendId))) {
                userFriendDoc.getFriends().add(new FriendEntry(friendId, friendUsername));
            }
            if (friendDoc.getFriends().stream().noneMatch(e -> e.getFriendId().equals(userId))) {
                friendDoc.getFriends().add(new FriendEntry(userId, userUsername));
            }
            userFriendDoc.updateTimestamp();
            friendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);
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

    public ResponseEntity<?> rejectFriendRequest(UUID userId, UUID friendId) {
        try {
            FriendRequestDocument userReq = getOrCreateFriendRequestDocument(userId);
            if (userReq.getReceiveRequest().stream().noneMatch(e -> e.getUserId().equals(friendId))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No pending friend request from this user");
            }
            userReq.getReceiveRequest().removeIf(e -> e.getUserId().equals(friendId));
            userReq.updateTimestamp();
            friendRequestMongoRepository.save(userReq);

            FriendRequestDocument friendReq = getOrCreateFriendRequestDocument(friendId);
            friendReq.getSendRequest().removeIf(e -> e.getUserId().equals(userId));
            friendReq.updateTimestamp();
            friendRequestMongoRepository.save(friendReq);

            return ResponseEntity.ok(Map.of(
                    "message", "Friend request rejected",
                    "friendId", friendId.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> blockFriend(UUID userId, UUID friendId) {
        try {
            Optional<UserDetails> friendOpt = registerRepository.findById(friendId);
            String friendUsername = friendOpt.map(u -> u.getUsername() != null ? u.getUsername() : "").orElse("");

            BlockedFriendDocument blockedDoc = getOrCreateBlockedDocument(userId);
            if (blockedDoc.getBlockedUsers().stream().anyMatch(e -> e.getUserId().equals(friendId))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is already blocked");
            }
            blockedDoc.getBlockedUsers().add(new FriendRequestEntry(friendId, friendUsername));
            blockedDoc.updateTimestamp();
            blockedFriendMongoRepository.save(blockedDoc);

            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);
            userFriendDoc.getFriends().removeIf(e -> e.getFriendId().equals(friendId));
            userFriendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);

            FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
            friendDoc.getFriends().removeIf(e -> e.getFriendId().equals(userId));
            friendDoc.updateTimestamp();
            friendMongoRepository.save(friendDoc);

            FriendRequestDocument userReq = getOrCreateFriendRequestDocument(userId);
            FriendRequestDocument friendReq = getOrCreateFriendRequestDocument(friendId);
            userReq.getSendRequest().removeIf(e -> e.getUserId().equals(friendId));
            userReq.getReceiveRequest().removeIf(e -> e.getUserId().equals(friendId));
            friendReq.getSendRequest().removeIf(e -> e.getUserId().equals(userId));
            friendReq.getReceiveRequest().removeIf(e -> e.getUserId().equals(userId));
            userReq.updateTimestamp();
            friendReq.updateTimestamp();
            friendRequestMongoRepository.save(userReq);
            friendRequestMongoRepository.save(friendReq);

            return ResponseEntity.ok(Map.of(
                    "message", "User blocked successfully",
                    "friendId", friendId.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> unblockFriend(UUID userId, UUID friendId) {
        try {
            BlockedFriendDocument blockedDoc = getOrCreateBlockedDocument(userId);
            if (blockedDoc.getBlockedUsers().stream().noneMatch(e -> e.getUserId().equals(friendId))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not blocked");
            }
            blockedDoc.getBlockedUsers().removeIf(e -> e.getUserId().equals(friendId));
            blockedDoc.updateTimestamp();
            blockedFriendMongoRepository.save(blockedDoc);

            return ResponseEntity.ok(Map.of(
                    "message", "User unblocked successfully",
                    "friendId", friendId.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> removeFriend(UUID userId, UUID friendId) {
        try {
            FriendDocument userFriendDoc = getOrCreateFriendDocument(userId);
            if (userFriendDoc.getFriends().stream().noneMatch(e -> e.getFriendId().equals(friendId))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not your friend");
            }
            userFriendDoc.getFriends().removeIf(e -> e.getFriendId().equals(friendId));
            userFriendDoc.updateTimestamp();
            friendMongoRepository.save(userFriendDoc);

            FriendDocument friendDoc = getOrCreateFriendDocument(friendId);
            friendDoc.getFriends().removeIf(e -> e.getFriendId().equals(userId));
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

    public ResponseEntity<?> searchFriends(UUID userId, String searchQuery) {
        try {
            Optional<FriendDocument> opt = friendMongoRepository.findByUserId(userId);
            if (opt.isEmpty() || opt.get().getFriends().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            String q = searchQuery == null ? "" : searchQuery.toLowerCase();
            String query = q;
            List<FriendInfo> list = opt.get().getFriends().stream()
                    .filter(e -> e.getFriendUsername() != null && e.getFriendUsername().toLowerCase().contains(query))
                    .map(FriendService::toFriendInfo)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> cancelSentRequest(UUID userId, UUID friendId) {
        try {
            FriendRequestDocument userReq = getOrCreateFriendRequestDocument(userId);
            if (userReq.getSendRequest().stream().noneMatch(e -> e.getUserId().equals(friendId))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No pending sent request to this user");
            }
            userReq.getSendRequest().removeIf(e -> e.getUserId().equals(friendId));
            userReq.updateTimestamp();
            friendRequestMongoRepository.save(userReq);

            FriendRequestDocument friendReq = getOrCreateFriendRequestDocument(friendId);
            friendReq.getReceiveRequest().removeIf(e -> e.getUserId().equals(userId));
            friendReq.updateTimestamp();
            friendRequestMongoRepository.save(friendReq);

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
