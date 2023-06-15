package com.example.mp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.mp.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  
  boolean existsByUsername(String username);
  boolean existsByEmail(String email);
  User findByUsername(String username);
}
