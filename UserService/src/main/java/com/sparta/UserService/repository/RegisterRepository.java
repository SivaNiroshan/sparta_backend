package com.sparta.UserService.repository;

import com.sparta.UserService.model.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RegisterRepository  extends JpaRepository<UserDetails,UUID> {

}
