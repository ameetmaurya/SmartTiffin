package com.example.NutriDeliver.repository;


import com.example.NutriDeliver.model.MenuPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MenuPlanRepository extends JpaRepository<MenuPlan, Long> {
    Optional<MenuPlan> findByActiveTrue();
}