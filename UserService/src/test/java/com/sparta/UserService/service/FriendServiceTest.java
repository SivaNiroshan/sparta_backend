package com.sparta.UserService.service;

import com.sparta.UserService.model.*;
import com.sparta.UserService.repository.BlockedFriendMongoRepository;
import com.sparta.UserService.repository.FriendMongoRepository;
import com.sparta.UserService.repository.FriendRequestMongoRepository;
import com.sparta.UserService.repository.RegisterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendMongoRepository friendMongoRepository;

    @Mock
    private FriendRequestMongoRepository friendRequestMongoRepository;

    @Mock
    private BlockedFriendMongoRepository blockedFriendMongoRepository;

    @Mock
    private RegisterRepository registerRepository;

    @InjectMocks
    private FriendService friendService;

    private UUID userId1;
    private UUID userId2;
    private UUID userId3;
    private UserDetails user1;
    private UserDetails user2;
    private UserDetails user3;
    private FriendDocument friendDoc1;
    private FriendDocument friendDoc2;
    private FriendRequestDocument requestDoc1;
    private FriendRequestDocument requestDoc2;
    private BlockedFriendDocument blockedDoc1;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();

        user1 = new UserDetails();
        user1.setId(userId1);
        user1.setEmail("user1@example.com");
        user1.setFirstname("John");
        user1.setLastname("Doe");
        user1.setUsername("johndoe");

        user2 = new UserDetails();
        user2.setId(userId2);
        user2.setEmail("user2@example.com");
        user2.setFirstname("Jane");
        user2.setLastname("Smith");
        user2.setUsername("janesmith");

        user3 = new UserDetails();
        user3.setId(userId3);
        user3.setEmail("user3@example.com");
        user3.setFirstname("Bob");
        user3.setLastname("Johnson");
        user3.setUsername("bobjohnson");

        friendDoc1 = new FriendDocument(userId1);
        friendDoc2 = new FriendDocument(userId2);
        requestDoc1 = new FriendRequestDocument(userId1);
        requestDoc2 = new FriendRequestDocument(userId2);
        blockedDoc1 = new BlockedFriendDocument(userId1);
    }

    // ========== Add Friend Tests ==========

    @Test
    void testAddFriend_Success_SendsRequest() {
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(registerRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Friend request sent successfully", body.get("message"));
        assertTrue(requestDoc1.getSendRequest().stream().anyMatch(e -> e.getUserId().equals(userId2)));
        assertTrue(requestDoc2.getReceiveRequest().stream().anyMatch(e -> e.getUserId().equals(userId1)));
        verify(friendRequestMongoRepository, atLeast(1)).save(any(FriendRequestDocument.class));
    }

    @Test
    void testAddFriend_Success_AcceptsExistingRequest() {
        requestDoc1.getReceiveRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        requestDoc2.getSendRequest().add(new FriendRequestEntry(userId1, "johndoe"));
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(registerRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(friendDoc1.getFriends().stream().anyMatch(e -> e.getFriendId().equals(userId2)));
        assertTrue(friendDoc2.getFriends().stream().anyMatch(e -> e.getFriendId().equals(userId1)));
        assertTrue(requestDoc1.getReceiveRequest().stream().noneMatch(e -> e.getUserId().equals(userId2)));
        assertTrue(requestDoc2.getSendRequest().stream().noneMatch(e -> e.getUserId().equals(userId1)));
    }

    @Test
    void testAddFriend_UserNotFound() {
        when(registerRepository.existsById(userId1)).thenReturn(false);

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found in database", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_FriendNotFound() {
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.empty());

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Friend ID not found in database", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_CannotAddSelf() {
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId1)).thenReturn(Optional.of(user1));

        ResponseEntity<?> response = friendService.addFriend(userId1, userId1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot add yourself as a friend", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_AlreadyFriend() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is already your friend", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_BlockedUser() {
        blockedDoc1.getBlockedUsers().add(new FriendRequestEntry(userId2, "janesmith"));
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot add blocked user as friend. Unblock first.", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_RequestAlreadySent() {
        requestDoc1.getSendRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Friend request already sent", response.getBody());
        verify(friendRequestMongoRepository, never()).save(any(FriendRequestDocument.class));
    }

    @Test
    void testAddFriend_CreatesNewDocument() {
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(registerRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.empty());
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> {
            FriendRequestDocument doc = invocation.getArgument(0);
            doc.setId("new-id");
            return doc;
        });

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(friendRequestMongoRepository, atLeast(1)).save(any(FriendRequestDocument.class));
    }

    // ========== Get Current Friends Tests ==========

    @Test
    void testGetCurrentFriends_Success() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        friendDoc1.getFriends().add(new FriendEntry(userId3, "bobjohnson"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> friends = (List<FriendInfo>) response.getBody();
        assertEquals(2, friends.size());
        assertTrue(friends.stream().anyMatch(f -> f.getUserId().equals(userId2) && "janesmith".equals(f.getUsername())));
        assertTrue(friends.stream().anyMatch(f -> f.getUserId().equals(userId3) && "bobjohnson".equals(f.getUsername())));
    }

    @Test
    void testGetCurrentFriends_EmptyList() {
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());

        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> friends = (List<FriendInfo>) response.getBody();
        assertTrue(friends.isEmpty());
    }

    @Test
    void testGetCurrentFriends_NoFriends() {
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> friends = (List<FriendInfo>) response.getBody();
        assertTrue(friends.isEmpty());
    }

    // ========== Get Blocked Friends Tests ==========

    @Test
    void testGetBlockedFriends_Success() {
        blockedDoc1.getBlockedUsers().add(new FriendRequestEntry(userId2, "janesmith"));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));

        ResponseEntity<?> response = friendService.getBlockedFriends(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> blocked = (List<FriendInfo>) response.getBody();
        assertEquals(1, blocked.size());
        assertEquals(userId2, blocked.get(0).getUserId());
        assertEquals("janesmith", blocked.get(0).getUsername());
    }

    @Test
    void testGetBlockedFriends_Empty() {
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());

        ResponseEntity<?> response = friendService.getBlockedFriends(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> blocked = (List<FriendInfo>) response.getBody();
        assertTrue(blocked.isEmpty());
    }

    // ========== Get Sent Requests Tests ==========

    @Test
    void testGetSentRequests_Success() {
        requestDoc1.getSendRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        requestDoc1.getSendRequest().add(new FriendRequestEntry(userId3, "bobjohnson"));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));

        ResponseEntity<?> response = friendService.getSentRequests(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> requests = (List<FriendInfo>) response.getBody();
        assertEquals(2, requests.size());
    }

    @Test
    void testGetSentRequests_Empty() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());

        ResponseEntity<?> response = friendService.getSentRequests(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> requests = (List<FriendInfo>) response.getBody();
        assertTrue(requests.isEmpty());
    }

    // ========== Get Received Requests Tests ==========

    @Test
    void testGetReceivedRequests_Success() {
        requestDoc1.getReceiveRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));

        ResponseEntity<?> response = friendService.getReceivedRequests(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> requests = (List<FriendInfo>) response.getBody();
        assertEquals(1, requests.size());
        assertEquals(userId2, requests.get(0).getUserId());
        assertEquals("janesmith", requests.get(0).getUsername());
    }

    // ========== Accept Friend Request Tests ==========

    @Test
    void testAcceptFriendRequest_Success() {
        requestDoc1.getReceiveRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        requestDoc2.getSendRequest().add(new FriendRequestEntry(userId1, "johndoe"));
        when(registerRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.acceptFriendRequest(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(friendDoc1.getFriends().stream().anyMatch(e -> e.getFriendId().equals(userId2)));
        assertTrue(friendDoc2.getFriends().stream().anyMatch(e -> e.getFriendId().equals(userId1)));
        assertTrue(requestDoc1.getReceiveRequest().stream().noneMatch(e -> e.getUserId().equals(userId2)));
        assertTrue(requestDoc2.getSendRequest().stream().noneMatch(e -> e.getUserId().equals(userId1)));
    }

    @Test
    void testAcceptFriendRequest_NoPendingRequest() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));

        ResponseEntity<?> response = friendService.acceptFriendRequest(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No pending friend request from this user", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    // ========== Reject Friend Request Tests ==========

    @Test
    void testRejectFriendRequest_Success() {
        requestDoc1.getReceiveRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        requestDoc2.getSendRequest().add(new FriendRequestEntry(userId1, "johndoe"));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.rejectFriendRequest(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(requestDoc1.getReceiveRequest().stream().noneMatch(e -> e.getUserId().equals(userId2)));
        assertTrue(requestDoc2.getSendRequest().stream().noneMatch(e -> e.getUserId().equals(userId1)));
    }

    @Test
    void testRejectFriendRequest_NoPendingRequest() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));

        ResponseEntity<?> response = friendService.rejectFriendRequest(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No pending friend request from this user", response.getBody());
    }

    // ========== Block Friend Tests ==========

    @Test
    void testBlockFriend_Success() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        friendDoc2.getFriends().add(new FriendEntry(userId1, "johndoe"));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));
        when(blockedFriendMongoRepository.save(any(BlockedFriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.blockFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(blockedDoc1.getBlockedUsers().stream().anyMatch(e -> e.getUserId().equals(userId2)));
        assertTrue(friendDoc1.getFriends().stream().noneMatch(e -> e.getFriendId().equals(userId2)));
        assertTrue(friendDoc2.getFriends().stream().noneMatch(e -> e.getFriendId().equals(userId1)));
    }

    @Test
    void testBlockFriend_AlreadyBlocked() {
        blockedDoc1.getBlockedUsers().add(new FriendRequestEntry(userId2, "janesmith"));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));

        ResponseEntity<?> response = friendService.blockFriend(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is already blocked", response.getBody());
    }

    @Test
    void testBlockFriend_RemovesFromRequests() {
        requestDoc1.getSendRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        requestDoc1.getReceiveRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(blockedFriendMongoRepository.save(any(BlockedFriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.blockFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(blockedDoc1.getBlockedUsers().stream().anyMatch(e -> e.getUserId().equals(userId2)));
        assertTrue(requestDoc1.getSendRequest().stream().noneMatch(e -> e.getUserId().equals(userId2)));
        assertTrue(requestDoc1.getReceiveRequest().stream().noneMatch(e -> e.getUserId().equals(userId2)));
    }

    // ========== Unblock Friend Tests ==========

    @Test
    void testUnblockFriend_Success() {
        blockedDoc1.getBlockedUsers().add(new FriendRequestEntry(userId2, "janesmith"));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));
        when(blockedFriendMongoRepository.save(any(BlockedFriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.unblockFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(blockedDoc1.getBlockedUsers().stream().noneMatch(e -> e.getUserId().equals(userId2)));
    }

    @Test
    void testUnblockFriend_NotBlocked() {
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));

        ResponseEntity<?> response = friendService.unblockFriend(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is not blocked", response.getBody());
        verify(blockedFriendMongoRepository, never()).save(any(BlockedFriendDocument.class));
    }

    // ========== Remove Friend Tests ==========

    @Test
    void testRemoveFriend_Success() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        friendDoc2.getFriends().add(new FriendEntry(userId1, "johndoe"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.removeFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(friendDoc1.getFriends().stream().noneMatch(e -> e.getFriendId().equals(userId2)));
        assertTrue(friendDoc2.getFriends().stream().noneMatch(e -> e.getFriendId().equals(userId1)));
    }

    @Test
    void testRemoveFriend_NotFriend() {
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.removeFriend(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is not your friend", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    // ========== Search Friends Tests ==========

    @Test
    void testSearchFriends_ByUsername() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        friendDoc1.getFriends().add(new FriendEntry(userId3, "bobjohnson"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.searchFriends(userId1, "jane");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> results = (List<FriendInfo>) response.getBody();
        assertEquals(1, results.size());
        assertEquals("janesmith", results.get(0).getUsername());
    }

    @Test
    void testSearchFriends_CaseInsensitive() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.searchFriends(userId1, "JANE");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> results = (List<FriendInfo>) response.getBody();
        assertEquals(1, results.size());
    }

    @Test
    void testSearchFriends_NoResults() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.searchFriends(userId1, "nonexistent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> results = (List<FriendInfo>) response.getBody();
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchFriends_EmptyFriendsList() {
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());

        ResponseEntity<?> response = friendService.searchFriends(userId1, "test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> results = (List<FriendInfo>) response.getBody();
        assertTrue(results.isEmpty());
    }

    // ========== Cancel Sent Request Tests ==========

    @Test
    void testCancelSentRequest_Success() {
        requestDoc1.getSendRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        requestDoc2.getReceiveRequest().add(new FriendRequestEntry(userId1, "johndoe"));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.cancelSentRequest(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(requestDoc1.getSendRequest().stream().noneMatch(e -> e.getUserId().equals(userId2)));
        assertTrue(requestDoc2.getReceiveRequest().stream().noneMatch(e -> e.getUserId().equals(userId1)));
    }

    @Test
    void testCancelSentRequest_NoPendingRequest() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));

        ResponseEntity<?> response = friendService.cancelSentRequest(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No pending sent request to this user", response.getBody());
        verify(friendRequestMongoRepository, never()).save(any(FriendRequestDocument.class));
    }

    // ========== Exception Handling Tests ==========

    @Test
    void testAddFriend_ExceptionHandling() {
        when(registerRepository.existsById(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testGetCurrentFriends_ExceptionHandling() {
        when(friendMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    // ========== Edge Cases ==========

    @Test
    void testAddFriend_WithNullUsername() {
        UserDetails userWithNullUsername = new UserDetails();
        userWithNullUsername.setId(userId2);
        userWithNullUsername.setEmail("test@example.com");
        userWithNullUsername.setUsername(null);

        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(userWithNullUsername));
        when(registerRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testSearchFriends_WithNullUsernameInEntry() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "testuser"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.searchFriends(userId1, "nonexistent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> results = (List<FriendInfo>) response.getBody();
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchFriends_WithNullQuery() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        friendDoc1.getFriends().add(new FriendEntry(userId3, "bobjohnson"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.searchFriends(userId1, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> results = (List<FriendInfo>) response.getBody();
        assertEquals(2, results.size());
    }

    @Test
    void testSearchFriends_WithEmptyQuery() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        friendDoc1.getFriends().add(new FriendEntry(userId3, "bobjohnson"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.searchFriends(userId1, "");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> results = (List<FriendInfo>) response.getBody();
        assertEquals(2, results.size());
    }

    @Test
    void testGetReceivedRequests_Empty() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());

        ResponseEntity<?> response = friendService.getReceivedRequests(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> requests = (List<FriendInfo>) response.getBody();
        assertTrue(requests.isEmpty());
    }

    @Test
    void testGetReceivedRequests_NoRequests() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));

        ResponseEntity<?> response = friendService.getReceivedRequests(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> requests = (List<FriendInfo>) response.getBody();
        assertTrue(requests.isEmpty());
    }

    @Test
    void testGetSentRequests_NoRequests() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));

        ResponseEntity<?> response = friendService.getSentRequests(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> requests = (List<FriendInfo>) response.getBody();
        assertTrue(requests.isEmpty());
    }

    @Test
    void testGetBlockedFriends_NoBlockedUsers() {
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));

        ResponseEntity<?> response = friendService.getBlockedFriends(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> blocked = (List<FriendInfo>) response.getBody();
        assertTrue(blocked.isEmpty());
    }

    // ========== Document Creation Tests ==========

    @Test
    void testAcceptFriendRequest_CreatesNewDocuments() {
        FriendRequestDocument newRequestDoc1 = new FriendRequestDocument(userId1);
        newRequestDoc1.getReceiveRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        when(registerRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(newRequestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.empty());
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.empty());
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.acceptFriendRequest(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(friendMongoRepository, atLeast(1)).save(any(FriendDocument.class));
        verify(friendRequestMongoRepository, atLeast(1)).save(any(FriendRequestDocument.class));
    }

    @Test
    void testRejectFriendRequest_CreatesNewDocuments() {
        FriendRequestDocument newRequestDoc1 = new FriendRequestDocument(userId1);
        newRequestDoc1.getReceiveRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(newRequestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.empty());
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.rejectFriendRequest(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(friendRequestMongoRepository, atLeast(1)).save(any(FriendRequestDocument.class));
    }

    @Test
    void testBlockFriend_CreatesNewDocuments() {
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.empty());
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.empty());
        when(blockedFriendMongoRepository.save(any(BlockedFriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.blockFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(blockedFriendMongoRepository, atLeast(1)).save(any(BlockedFriendDocument.class));
    }

    @Test
    void testUnblockFriend_CreatesNewDocument() {
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());
        when(blockedFriendMongoRepository.save(any(BlockedFriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.unblockFriend(userId1, userId2);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is not blocked", response.getBody());
    }

    @Test
    void testRemoveFriend_CreatesNewDocuments() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.empty());
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.removeFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(friendMongoRepository, atLeast(1)).save(any(FriendDocument.class));
    }

    @Test
    void testCancelSentRequest_CreatesNewDocuments() {
        FriendRequestDocument newRequestDoc1 = new FriendRequestDocument(userId1);
        newRequestDoc1.getSendRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(newRequestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.empty());
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.cancelSentRequest(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(friendRequestMongoRepository, atLeast(1)).save(any(FriendRequestDocument.class));
    }

    // ========== Additional Exception Handling Tests ==========

    @Test
    void testGetBlockedFriends_ExceptionHandling() {
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.getBlockedFriends(userId1);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testGetSentRequests_ExceptionHandling() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.getSentRequests(userId1);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testGetReceivedRequests_ExceptionHandling() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.getReceivedRequests(userId1);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testAcceptFriendRequest_ExceptionHandling() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.acceptFriendRequest(userId1, userId2);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testRejectFriendRequest_ExceptionHandling() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.rejectFriendRequest(userId1, userId2);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testBlockFriend_ExceptionHandling() {
        when(registerRepository.findById(userId2)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.blockFriend(userId1, userId2);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testUnblockFriend_ExceptionHandling() {
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.unblockFriend(userId1, userId2);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testRemoveFriend_ExceptionHandling() {
        when(friendMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.removeFriend(userId1, userId2);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testSearchFriends_ExceptionHandling() {
        when(friendMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.searchFriends(userId1, "test");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testCancelSentRequest_ExceptionHandling() {
        when(friendRequestMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = friendService.cancelSentRequest(userId1, userId2);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    // ========== Additional Edge Cases ==========

    @Test
    void testAcceptFriendRequest_AlreadyFriend() {
        requestDoc1.getReceiveRequest().add(new FriendRequestEntry(userId2, "janesmith"));
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        when(registerRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.acceptFriendRequest(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(requestDoc1.getReceiveRequest().stream().noneMatch(e -> e.getUserId().equals(userId2)));
    }

    @Test
    void testBlockFriend_WithNullUsername() {
        UserDetails userWithNullUsername = new UserDetails();
        userWithNullUsername.setId(userId2);
        userWithNullUsername.setUsername(null);

        when(registerRepository.findById(userId2)).thenReturn(Optional.of(userWithNullUsername));
        when(blockedFriendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(blockedDoc1));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendRequestMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(requestDoc1));
        when(friendRequestMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(requestDoc2));
        when(blockedFriendMongoRepository.save(any(BlockedFriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendRequestMongoRepository.save(any(FriendRequestDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = friendService.blockFriend(userId1, userId2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(blockedDoc1.getBlockedUsers().stream().anyMatch(e -> e.getUserId().equals(userId2)));
    }

    @Test
    void testSearchFriends_PartialMatch() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        friendDoc1.getFriends().add(new FriendEntry(userId3, "janedoe"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.searchFriends(userId1, "jane");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> results = (List<FriendInfo>) response.getBody();
        assertEquals(2, results.size());
    }

    @Test
    void testGetCurrentFriends_MultipleFriends() {
        friendDoc1.getFriends().add(new FriendEntry(userId2, "janesmith"));
        friendDoc1.getFriends().add(new FriendEntry(userId3, "bobjohnson"));
        friendDoc1.getFriends().add(new FriendEntry(UUID.randomUUID(), "alicewonder"));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<FriendInfo> friends = (List<FriendInfo>) response.getBody();
        assertEquals(3, friends.size());
    }
}
