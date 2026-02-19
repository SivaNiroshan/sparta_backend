package com.sparta.UserService.repository;

import com.sparta.UserService.model.FriendDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MongoDB Repository for the friends collection.
 */
@Repository
public interface FriendMongoRepository extends MongoRepository<FriendDocument, String> {

    Optional<FriendDocument> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("{ 'friends.friendId': ?0 }")
    List<FriendDocument> findUsersWhoHaveAsFriend(UUID friendId);
}
