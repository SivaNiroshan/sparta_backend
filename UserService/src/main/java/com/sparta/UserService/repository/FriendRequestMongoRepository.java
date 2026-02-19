package com.sparta.UserService.repository;

import com.sparta.UserService.model.FriendRequestDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MongoDB Repository for the friend_requests collection.
 */
@Repository
public interface FriendRequestMongoRepository extends MongoRepository<FriendRequestDocument, String> {

    Optional<FriendRequestDocument> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("{ 'sendRequest.userId': ?0 }")
    List<FriendRequestDocument> findUsersWhoSentRequestTo(UUID userId);

    @Query("{ 'receiveRequest.userId': ?0 }")
    List<FriendRequestDocument> findUsersWhoReceivedRequestFrom(UUID userId);
}
