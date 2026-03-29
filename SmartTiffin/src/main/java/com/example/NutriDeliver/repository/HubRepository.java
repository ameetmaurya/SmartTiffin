package com.example.NutriDeliver.repository;

import com.example.NutriDeliver.model.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HubRepository extends JpaRepository<Hub, Long> {
    List<Hub> findByActiveTrue();
}