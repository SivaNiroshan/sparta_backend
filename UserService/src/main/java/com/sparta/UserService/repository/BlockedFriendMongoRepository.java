package com.sparta.UserService.repository;

import com.sparta.UserService.model.BlockedFriendDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MongoDB Repository for the blocked_friends collection.
 */
@Repository
public interface BlockedFriendMongoRepository extends MongoRepository<BlockedFriendDocument, String> {

    Optional<BlockedFriendDocument> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("{ 'blockedUsers.userId': ?0 }")
    List<BlockedFriendDocument> findUsersWhoBlocked(UUID userId);
}
