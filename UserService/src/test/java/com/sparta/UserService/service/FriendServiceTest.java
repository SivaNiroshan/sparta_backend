package com.sparta.UserService.service;

import com.sparta.UserService.model.FriendDocument;
import com.sparta.UserService.model.UserDetails;
import com.sparta.UserService.repository.FriendMongoRepository;
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
    }

    // ========== Add Friend Tests ==========

    @Test
    void testAddFriend_Success_SendsRequest() {
        // Arrange
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Friend request sent successfully", body.get("message"));
        assertTrue(friendDoc1.getSentRequests().contains(userId2));
        assertTrue(friendDoc2.getReceivedRequests().contains(userId1));
        verify(friendMongoRepository, times(2)).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_Success_AcceptsExistingRequest() {
        // Arrange
        friendDoc1.getReceivedRequests().add(userId2);
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(friendDoc1.getCurrentFriends().contains(userId2));
        assertFalse(friendDoc1.getReceivedRequests().contains(userId2));
        assertTrue(friendDoc2.getCurrentFriends().contains(userId1));
        assertFalse(friendDoc2.getSentRequests().contains(userId1));
        verify(friendMongoRepository, times(2)).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_UserNotFound() {
        // Arrange
        when(registerRepository.existsById(userId1)).thenReturn(false);

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found in database", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_FriendNotFound() {
        // Arrange
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Friend ID not found in database", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_CannotAddSelf() {
        // Arrange
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId1)).thenReturn(Optional.of(user1));

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId1);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot add yourself as a friend", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_AlreadyFriend() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is already your friend", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_BlockedUser() {
        // Arrange
        friendDoc1.getBlockedFriends().add(userId2);
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot add blocked user as friend. Unblock first.", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_RequestAlreadySent() {
        // Arrange
        friendDoc1.getSentRequests().add(userId2);
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Friend request already sent", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testAddFriend_CreatesNewDocument() {
        // Arrange
        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.empty());
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> {
            FriendDocument doc = invocation.getArgument(0);
            doc.setId("new-id");
            return doc;
        });

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // getOrCreateFriendDocument saves when creating new docs, so we get:
        // 1. Save for creating user1's doc
        // 2. Save for creating user2's doc  
        // 3. Save for updating user1's doc with sent request
        // 4. Save for updating user2's doc with received request
        verify(friendMongoRepository, atLeast(2)).save(any(FriendDocument.class));
    }

    // ========== Get Current Friends Tests ==========

    @Test
    void testGetCurrentFriends_Success() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        friendDoc1.getCurrentFriends().add(userId3);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(registerRepository.findById(userId3)).thenReturn(Optional.of(user3));

        // Act
        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> friends = (List<UserDetails>) response.getBody();
        assertNotNull(friends);
        assertEquals(2, friends.size());
        assertTrue(friends.contains(user2));
        assertTrue(friends.contains(user3));
    }

    @Test
    void testGetCurrentFriends_EmptyList() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> friends = (List<UserDetails>) response.getBody();
        assertNotNull(friends);
        assertTrue(friends.isEmpty());
    }

    @Test
    void testGetCurrentFriends_NoFriends() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> friends = (List<UserDetails>) response.getBody();
        assertNotNull(friends);
        assertTrue(friends.isEmpty());
    }

    @Test
    void testGetCurrentFriends_FriendNotFoundInPostgreSQL() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> friends = (List<UserDetails>) response.getBody();
        assertNotNull(friends);
        assertTrue(friends.isEmpty()); // Filtered out non-existent friends
    }

    // ========== Get Blocked Friends Tests ==========

    @Test
    void testGetBlockedFriends_Success() {
        // Arrange
        friendDoc1.getBlockedFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));

        // Act
        ResponseEntity<?> response = friendService.getBlockedFriends(userId1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> blocked = (List<UserDetails>) response.getBody();
        assertNotNull(blocked);
        assertEquals(1, blocked.size());
        assertEquals(user2, blocked.get(0));
    }

    @Test
    void testGetBlockedFriends_Empty() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = friendService.getBlockedFriends(userId1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> blocked = (List<UserDetails>) response.getBody();
        assertNotNull(blocked);
        assertTrue(blocked.isEmpty());
    }

    // ========== Get Sent Requests Tests ==========

    @Test
    void testGetSentRequests_Success() {
        // Arrange
        friendDoc1.getSentRequests().add(userId2);
        friendDoc1.getSentRequests().add(userId3);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(registerRepository.findById(userId3)).thenReturn(Optional.of(user3));

        // Act
        ResponseEntity<?> response = friendService.getSentRequests(userId1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> requests = (List<UserDetails>) response.getBody();
        assertNotNull(requests);
        assertEquals(2, requests.size());
    }

    @Test
    void testGetSentRequests_Empty() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = friendService.getSentRequests(userId1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> requests = (List<UserDetails>) response.getBody();
        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    // ========== Get Received Requests Tests ==========

    @Test
    void testGetReceivedRequests_Success() {
        // Arrange
        friendDoc1.getReceivedRequests().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));

        // Act
        ResponseEntity<?> response = friendService.getReceivedRequests(userId1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> requests = (List<UserDetails>) response.getBody();
        assertNotNull(requests);
        assertEquals(1, requests.size());
        assertEquals(user2, requests.get(0));
    }

    // ========== Accept Friend Request Tests ==========

    @Test
    void testAcceptFriendRequest_Success() {
        // Arrange
        friendDoc1.getReceivedRequests().add(userId2);
        friendDoc2.getSentRequests().add(userId1);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.acceptFriendRequest(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(friendDoc1.getCurrentFriends().contains(userId2));
        assertFalse(friendDoc1.getReceivedRequests().contains(userId2));
        assertTrue(friendDoc2.getCurrentFriends().contains(userId1));
        assertFalse(friendDoc2.getSentRequests().contains(userId1));
        verify(friendMongoRepository, times(2)).save(any(FriendDocument.class));
    }

    @Test
    void testAcceptFriendRequest_NoPendingRequest() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.acceptFriendRequest(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No pending friend request from this user", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    // ========== Reject Friend Request Tests ==========

    @Test
    void testRejectFriendRequest_Success() {
        // Arrange
        friendDoc1.getReceivedRequests().add(userId2);
        friendDoc2.getSentRequests().add(userId1);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.rejectFriendRequest(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(friendDoc1.getReceivedRequests().contains(userId2));
        assertFalse(friendDoc2.getSentRequests().contains(userId1));
        assertFalse(friendDoc1.getCurrentFriends().contains(userId2));
        verify(friendMongoRepository, times(2)).save(any(FriendDocument.class));
    }

    @Test
    void testRejectFriendRequest_NoPendingRequest() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.rejectFriendRequest(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No pending friend request from this user", response.getBody());
    }

    // ========== Block Friend Tests ==========

    @Test
    void testBlockFriend_Success() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        friendDoc2.getCurrentFriends().add(userId1);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.blockFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(friendDoc1.getBlockedFriends().contains(userId2));
        assertFalse(friendDoc1.getCurrentFriends().contains(userId2));
        assertFalse(friendDoc2.getCurrentFriends().contains(userId1));
        verify(friendMongoRepository, times(2)).save(any(FriendDocument.class));
    }

    @Test
    void testBlockFriend_AlreadyBlocked() {
        // Arrange
        friendDoc1.getBlockedFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.blockFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is already blocked", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    @Test
    void testBlockFriend_RemovesFromRequests() {
        // Arrange
        friendDoc1.getSentRequests().add(userId2);
        friendDoc1.getReceivedRequests().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.blockFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(friendDoc1.getBlockedFriends().contains(userId2));
        assertFalse(friendDoc1.getSentRequests().contains(userId2));
        assertFalse(friendDoc1.getReceivedRequests().contains(userId2));
    }

    // ========== Unblock Friend Tests ==========

    @Test
    void testUnblockFriend_Success() {
        // Arrange
        friendDoc1.getBlockedFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.unblockFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(friendDoc1.getBlockedFriends().contains(userId2));
        verify(friendMongoRepository, times(1)).save(any(FriendDocument.class));
    }

    @Test
    void testUnblockFriend_NotBlocked() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.unblockFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is not blocked", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    // ========== Remove Friend Tests ==========

    @Test
    void testRemoveFriend_Success() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        friendDoc2.getCurrentFriends().add(userId1);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.removeFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(friendDoc1.getCurrentFriends().contains(userId2));
        assertFalse(friendDoc2.getCurrentFriends().contains(userId1));
        verify(friendMongoRepository, times(2)).save(any(FriendDocument.class));
    }

    @Test
    void testRemoveFriend_NotFriend() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.removeFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is not your friend", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    // ========== Search Friends Tests ==========

    @Test
    void testSearchFriends_ByUsername() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        friendDoc1.getCurrentFriends().add(userId3);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(registerRepository.findById(userId3)).thenReturn(Optional.of(user3));

        // Act
        ResponseEntity<?> response = friendService.searchFriends(userId1, "jane");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> results = (List<UserDetails>) response.getBody();
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(user2, results.get(0));
    }

    @Test
    void testSearchFriends_ByEmail() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));

        // Act
        ResponseEntity<?> response = friendService.searchFriends(userId1, "user2");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> results = (List<UserDetails>) response.getBody();
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchFriends_ByFirstname() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));

        // Act
        ResponseEntity<?> response = friendService.searchFriends(userId1, "Jane");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> results = (List<UserDetails>) response.getBody();
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchFriends_ByLastname() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));

        // Act
        ResponseEntity<?> response = friendService.searchFriends(userId1, "Smith");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> results = (List<UserDetails>) response.getBody();
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchFriends_CaseInsensitive() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));

        // Act
        ResponseEntity<?> response = friendService.searchFriends(userId1, "JANE");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> results = (List<UserDetails>) response.getBody();
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchFriends_NoResults() {
        // Arrange
        friendDoc1.getCurrentFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(user2));

        // Act
        ResponseEntity<?> response = friendService.searchFriends(userId1, "nonexistent");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> results = (List<UserDetails>) response.getBody();
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchFriends_EmptyFriendsList() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = friendService.searchFriends(userId1, "test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> results = (List<UserDetails>) response.getBody();
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ========== Cancel Sent Request Tests ==========

    @Test
    void testCancelSentRequest_Success() {
        // Arrange
        friendDoc1.getSentRequests().add(userId2);
        friendDoc2.getReceivedRequests().add(userId1);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.cancelSentRequest(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(friendDoc1.getSentRequests().contains(userId2));
        assertFalse(friendDoc2.getReceivedRequests().contains(userId1));
        verify(friendMongoRepository, times(2)).save(any(FriendDocument.class));
    }

    @Test
    void testCancelSentRequest_NoPendingRequest() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));

        // Act
        ResponseEntity<?> response = friendService.cancelSentRequest(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No pending sent request to this user", response.getBody());
        verify(friendMongoRepository, never()).save(any(FriendDocument.class));
    }

    // ========== Exception Handling Tests ==========

    @Test
    void testAddFriend_ExceptionHandling() {
        // Arrange
        when(registerRepository.existsById(userId1)).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    @Test
    void testGetCurrentFriends_ExceptionHandling() {
        // Arrange
        when(friendMongoRepository.findByUserId(userId1)).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = friendService.getCurrentFriends(userId1);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Server error"));
    }

    // ========== Edge Cases ==========

    @Test
    void testAddFriend_WithNullFieldsInUser() {
        // Arrange
        UserDetails userWithNulls = new UserDetails();
        userWithNulls.setId(userId2);
        userWithNulls.setEmail("test@example.com");
        userWithNulls.setFirstname(null);
        userWithNulls.setLastname(null);
        userWithNulls.setUsername(null);

        when(registerRepository.existsById(userId1)).thenReturn(true);
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(userWithNulls));
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(friendMongoRepository.findByUserId(userId2)).thenReturn(Optional.of(friendDoc2));
        when(friendMongoRepository.save(any(FriendDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = friendService.addFriend(userId1, userId2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testSearchFriends_WithNullFields() {
        // Arrange
        UserDetails userWithNulls = new UserDetails();
        userWithNulls.setId(userId2);
        userWithNulls.setEmail("test@example.com");
        userWithNulls.setFirstname(null);
        userWithNulls.setLastname(null);
        userWithNulls.setUsername("testuser");

        friendDoc1.getCurrentFriends().add(userId2);
        when(friendMongoRepository.findByUserId(userId1)).thenReturn(Optional.of(friendDoc1));
        when(registerRepository.findById(userId2)).thenReturn(Optional.of(userWithNulls));

        // Act
        ResponseEntity<?> response = friendService.searchFriends(userId1, "testuser");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<UserDetails> results = (List<UserDetails>) response.getBody();
        assertNotNull(results);
        assertEquals(1, results.size());
    }
}

