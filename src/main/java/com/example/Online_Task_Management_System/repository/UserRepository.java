package com.example.Online_Task_Management_System.repository;

import com.example.Online_Task_Management_System.entity.Users;
import com.example.Online_Task_Management_System.enums.Roles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users,Long> {
    Optional<Users> findByEmail(String email); // Use 'email' if that is the correct field for login
    boolean existsByEmail(String email);

    @Query(
            value = """
            SELECT * 
            FROM users 
            WHERE roles = :role
            """,
            countQuery = """
            SELECT COUNT(*) 
            FROM users 
            WHERE roles = :role
            """,
            nativeQuery = true
    )
    Page<Users> findByRoleNative(
            @Param("role") String role,
            Pageable pageable
    );
}
