package com.example.NutriDeliver.repository;

import com.example.NutriDeliver.model.User;
import com.example.NutriDeliver.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    // ── OPTIMIZED QUERIES FOR ADMIN DASHBOARD ──
    
    // ✅ FIXED: Changed 'String' to 'User.Role'
    List<User> findByRole(User.Role role);
    
    long countByRole(User.Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.verificationStatus = :status")
    long countByRoleAndVerificationStatus(@Param("role") User.Role role, @Param("status") VerificationStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'STUDENT' AND u.activeSubscription IS NOT NULL")
    long countActiveSubscriptions();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'STUDENT' AND u.hostelBlock = :block")
    long countStudentsByBlock(@Param("block") String block);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'STUDENT' AND u.createdAt >= :date")
    long countNewStudentsSince(@Param("date") LocalDateTime date);
}