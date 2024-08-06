package com.example.side.user.repository;

import com.example.side.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    /**
     * 이미 존재하는지 확인
     */
    boolean existsByUsername(String username);

    User findByEmail(String email);

    User findByVerificationToken(String token);

}
