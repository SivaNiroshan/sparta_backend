package com.sparta.UserService.controler;

import com.sparta.UserService.model.Friends;
import com.sparta.UserService.model.UserDetails;
import com.sparta.UserService.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("account/friend")
public class FriendController {
    @Autowired
    private FriendService friendserve;

    @PostMapping("/add")
    public  ResponseEntity<?> AddFriend(@RequestBody Friends friends){
        try{
            if(friends.getOwner()==null || friends.getFriend()==null){
                throw new IllegalArgumentException("Missing 'owner' or 'friend' field");
            }
            return  friendserve.AddFriend(friends);
        }
        catch(Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/get")
    public List<UserDetails> getFriends(@RequestParam UUID id) {
        return friendserve.getFriend(id);
    }

    @PostMapping("/status")
    public ResponseEntity<?> changeBlockFriend(@RequestBody Friends friends) {
        try{
            friendserve.blockFriendStatus(friends);
            return ResponseEntity.ok().build();
        }
        catch(Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());

        }

    }

    @DeleteMapping("/delfriend")
    public ResponseEntity<?> deleteFriend(@RequestBody Friends friends){
        try{
            friendserve.deleteFriend(friends);
            return ResponseEntity.ok().build();
        }
        catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }


}
