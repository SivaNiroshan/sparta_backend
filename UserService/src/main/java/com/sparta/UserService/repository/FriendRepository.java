package com.sparta.UserService.repository;

import com.sparta.UserService.model.Friends;
import com.sparta.UserService.model.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FriendRepository extends JpaRepository<Friends,Long> {
    @Query(value = """
    SELECT u.*
    FROM friends f
    JOIN user_details u
      ON (f.owner = :myid AND f.friend = u.id)
      OR (f.friend = :myid AND f.owner = u.id)
    WHERE f.block = false
    """, nativeQuery = true)
    List<UserDetails> findFriends(@Param("myid") UUID myid);

    @Modifying
    @Query("UPDATE Friends f SET f.block = :block WHERE " +
            "(f.owner = :owner AND f.friend = :friend)")
    int blockFriendship(@Param("owner") UUID owner, @Param("friend") UUID friend, @Param("block") boolean block);

    int deleteByOwnerAndFriend(UUID owner, UUID friend);


}
