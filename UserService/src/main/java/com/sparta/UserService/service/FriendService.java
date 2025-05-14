package com.sparta.UserService.service;

import com.sparta.UserService.model.Friends;
import com.sparta.UserService.model.UserDetails;
import com.sparta.UserService.repository.FriendRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FriendService {

    @Autowired
    private FriendRepository friendrepo;

    public ResponseEntity<?>AddFriend(Friends friend){
        try{
            friendrepo.save(friend);
            return ResponseEntity.ok().build();
        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }


    }

    public List<UserDetails> getFriend(UUID myid){
        return friendrepo.findFriends(myid);


    }


    @Transactional
    public void blockFriendStatus( Friends friends){
        if(friends.getOwner()==null || friends.getFriend()==null ){
            throw new IllegalArgumentException("owner or friend id not found");
        }
        int count= friendrepo.blockFriendship(friends.getOwner(),friends.getFriend(), friends.isBlock());
        if(count==0){
            throw  new RuntimeException("Block status can't be updated");
        }


    }

    @Transactional
    public void deleteFriend(Friends friends){
        if(friends.getOwner()==null || friends.getFriend()==null ){
            throw new IllegalArgumentException("owner or friend id not found");
        }
        int count=friendrepo.deleteByOwnerAndFriend(friends.getOwner(),friends.getFriend());
        if(count==0){
            throw new RuntimeException("Corresponding data not found");
        }


    }



}
