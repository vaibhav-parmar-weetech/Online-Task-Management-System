package com.example.Online_Task_Management_System.repository;

import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken,Long> {
    Optional<VerificationToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM VerificationToken v WHERE v.user = :user")
    void deleteByUser(@Param("user") Users user);

    Optional<VerificationToken> findByUser(Users user);
}
