package com.sparta.UserService.controler;

import com.sparta.UserService.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Friend Management", description = "API endpoints for managing friends, friend requests, and blocking users")
public class FriendController {
    
    @Autowired
    private FriendService friendService;

    /**
     * Add a new friend or send friend request
     * POST /account/friend/add?userId={userId}&friendId={friendId}
     */
    @Operation(
            summary = "Add friend or send friend request",
            description = "Send a friend request to another user. If the other user has already sent a request, they become friends automatically."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Friend request sent successfully or friendship established",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Friend request sent\", \"status\": \"pending\"}")
                    )
            )
    })
    @PostMapping("/add")
    public ResponseEntity<?> addFriend(
            @Parameter(description = "User ID of the requester", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam UUID userId,
            @Parameter(description = "User ID of the friend to add", required = true, example = "223e4567-e89b-12d3-a456-426614174001")
            @RequestParam UUID friendId) {
        return friendService.addFriend(userId, friendId);
    }

    /**
     * Get all current friends
     * GET /account/friend/current?userId={userId}
     */
    @Operation(
            summary = "Get current friends",
            description = "Retrieve all confirmed friends for a user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Friends list retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"friends\": [{\"userId\": \"uuid\", \"username\": \"friend1\", \"email\": \"friend1@example.com\"}]}")
                    )
            )
    })
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentFriends(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam UUID userId) {
        return friendService.getCurrentFriends(userId);
    }

    @Operation(
            summary = "Get blocked friends",
            description = "Retrieve all blocked users for a user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Blocked users list retrieved successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/blocked")
    public ResponseEntity<?> getBlockedFriends(
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId) {
        return friendService.getBlockedFriends(userId);
    }

    @Operation(
            summary = "Get sent friend requests",
            description = "Retrieve all pending friend requests sent by the user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sent requests retrieved successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/sent-requests")
    public ResponseEntity<?> getSentRequests(
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId) {
        return friendService.getSentRequests(userId);
    }

    @Operation(
            summary = "Get received friend requests",
            description = "Retrieve all pending friend requests received by the user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Received requests retrieved successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/received-requests")
    public ResponseEntity<?> getReceivedRequests(
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId) {
        return friendService.getReceivedRequests(userId);
    }

    @Operation(
            summary = "Accept friend request",
            description = "Accept a pending friend request from another user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Friend request accepted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Friend request accepted\"}")
                    )
            )
    })
    @PostMapping("/accept")
    public ResponseEntity<?> acceptFriendRequest(
            @Parameter(description = "User ID accepting the request", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "User ID of the friend request sender", required = true)
            @RequestParam UUID friendId) {
        return friendService.acceptFriendRequest(userId, friendId);
    }

    @Operation(
            summary = "Reject friend request",
            description = "Reject a pending friend request from another user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Friend request rejected successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/reject")
    public ResponseEntity<?> rejectFriendRequest(
            @Parameter(description = "User ID rejecting the request", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "User ID of the friend request sender", required = true)
            @RequestParam UUID friendId) {
        return friendService.rejectFriendRequest(userId, friendId);
    }

    @Operation(
            summary = "Cancel sent friend request",
            description = "Cancel a friend request that was previously sent."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Friend request cancelled successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/cancel-request")
    public ResponseEntity<?> cancelSentRequest(
            @Parameter(description = "User ID who sent the request", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "User ID of the friend request recipient", required = true)
            @RequestParam UUID friendId) {
        return friendService.cancelSentRequest(userId, friendId);
    }

    @Operation(
            summary = "Block a friend",
            description = "Block a user. This will remove them from friends list and block all interactions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User blocked successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/block")
    public ResponseEntity<?> blockFriend(
            @Parameter(description = "User ID blocking the friend", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "User ID to block", required = true)
            @RequestParam UUID friendId) {
        return friendService.blockFriend(userId, friendId);
    }

    @Operation(
            summary = "Unblock a user",
            description = "Unblock a previously blocked user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User unblocked successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/unblock")
    public ResponseEntity<?> unblockFriend(
            @Parameter(description = "User ID unblocking the friend", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "User ID to unblock", required = true)
            @RequestParam UUID friendId) {
        return friendService.unblockFriend(userId, friendId);
    }

    @Operation(
            summary = "Remove a friend (unfriend)",
            description = "Remove a user from friends list."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Friend removed successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFriend(
            @Parameter(description = "User ID removing the friend", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "User ID to remove from friends", required = true)
            @RequestParam UUID friendId) {
        return friendService.removeFriend(userId, friendId);
    }

    @Operation(
            summary = "Search friends",
            description = "Search for friends by username, email, firstname, or lastname."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search completed successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchFriends(
            @Parameter(description = "User ID performing the search", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Search query string", required = true, example = "john")
            @RequestParam String query) {
        return friendService.searchFriends(userId, query);
    }

    @Operation(
            summary = "Get friends (Legacy)",
            description = "Legacy endpoint for getting friends. Use /current instead."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Friends list retrieved successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/get")
    public ResponseEntity<?> getFriends(
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID id) {
        return friendService.getCurrentFriends(id);
    }
}
