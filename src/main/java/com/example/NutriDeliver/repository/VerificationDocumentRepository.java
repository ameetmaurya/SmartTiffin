package com.example.NutriDeliver.repository;

import com.example.NutriDeliver.model.VerificationDocument;
import com.example.NutriDeliver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, Long> {
    Optional<VerificationDocument> findByUser(User user);
}