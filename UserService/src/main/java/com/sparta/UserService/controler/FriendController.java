package com.sparta.UserService.controler;

import com.sparta.UserService.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Friend Controller
 * Handles all friend-related API endpoints
 */
@RestController
@RequestMapping("/account/friend")
public class FriendController {
    
    @Autowired
    private FriendService friendService;

    /**
     * Add a new friend or send friend request
     * POST /account/friend/add?userId={userId}&friendId={friendId}
     */
    @PostMapping("/add")
    public ResponseEntity<?> addFriend(
            @RequestParam UUID userId,
            @RequestParam UUID friendId) {
        return friendService.addFriend(userId, friendId);
    }

    /**
     * Get all current friends
     * GET /account/friend/current?userId={userId}
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentFriends(@RequestParam UUID userId) {
        return friendService.getCurrentFriends(userId);
    }

    /**
     * Get blocked friends
     * GET /account/friend/blocked?userId={userId}
     */
    @GetMapping("/blocked")
    public ResponseEntity<?> getBlockedFriends(@RequestParam UUID userId) {
        return friendService.getBlockedFriends(userId);
    }

    /**
     * Get sent friend requests (pending)
     * GET /account/friend/sent-requests?userId={userId}
     */
    @GetMapping("/sent-requests")
    public ResponseEntity<?> getSentRequests(@RequestParam UUID userId) {
        return friendService.getSentRequests(userId);
    }

    /**
     * Get received friend requests (pending)
     * GET /account/friend/received-requests?userId={userId}
     */
    @GetMapping("/received-requests")
    public ResponseEntity<?> getReceivedRequests(@RequestParam UUID userId) {
        return friendService.getReceivedRequests(userId);
    }

    /**
     * Accept a friend request
     * POST /account/friend/accept?userId={userId}&friendId={friendId}
     */
    @PostMapping("/accept")
    public ResponseEntity<?> acceptFriendRequest(
            @RequestParam UUID userId,
            @RequestParam UUID friendId) {
        return friendService.acceptFriendRequest(userId, friendId);
    }

    /**
     * Reject a friend request
     * POST /account/friend/reject?userId={userId}&friendId={friendId}
     */
    @PostMapping("/reject")
    public ResponseEntity<?> rejectFriendRequest(
            @RequestParam UUID userId,
            @RequestParam UUID friendId) {
        return friendService.rejectFriendRequest(userId, friendId);
    }

    /**
     * Cancel a sent friend request
     * POST /account/friend/cancel-request?userId={userId}&friendId={friendId}
     */
    @PostMapping("/cancel-request")
    public ResponseEntity<?> cancelSentRequest(
            @RequestParam UUID userId,
            @RequestParam UUID friendId) {
        return friendService.cancelSentRequest(userId, friendId);
    }

    /**
     * Block a friend
     * POST /account/friend/block?userId={userId}&friendId={friendId}
     */
    @PostMapping("/block")
    public ResponseEntity<?> blockFriend(
            @RequestParam UUID userId,
            @RequestParam UUID friendId) {
        return friendService.blockFriend(userId, friendId);
    }

    /**
     * Unblock a friend
     * POST /account/friend/unblock?userId={userId}&friendId={friendId}
     */
    @PostMapping("/unblock")
    public ResponseEntity<?> unblockFriend(
            @RequestParam UUID userId,
            @RequestParam UUID friendId) {
        return friendService.unblockFriend(userId, friendId);
    }

    /**
     * Remove a friend (unfriend)
     * DELETE /account/friend/remove?userId={userId}&friendId={friendId}
     */
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFriend(
            @RequestParam UUID userId,
            @RequestParam UUID friendId) {
        return friendService.removeFriend(userId, friendId);
    }

    /**
     * Search friends by username, email, firstname, or lastname
     * GET /account/friend/search?userId={userId}&query={searchQuery}
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchFriends(
            @RequestParam UUID userId,
            @RequestParam String query) {
        return friendService.searchFriends(userId, query);
    }

    /**
     * Legacy endpoint - kept for backward compatibility
     * GET /account/friend/get?id={userId}
     */
    @GetMapping("/get")
    public ResponseEntity<?> getFriends(@RequestParam UUID id) {
        return friendService.getCurrentFriends(id);
    }
}
