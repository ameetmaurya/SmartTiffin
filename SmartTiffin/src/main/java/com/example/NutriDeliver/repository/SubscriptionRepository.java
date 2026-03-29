package com.example.NutriDeliver.repository;

import com.example.NutriDeliver.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Use Optional so we never get NonUniqueResultException surprises
    Optional<Subscription> findByStudentId(Long studentId);

    List<Subscription> findByIsActiveTrue();
}