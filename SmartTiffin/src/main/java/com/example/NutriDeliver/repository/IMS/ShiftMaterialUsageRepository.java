package com.example.NutriDeliver.repository.IMS;

import com.example.NutriDeliver.model.Hub;
import com.example.NutriDeliver.model.IMS.ShiftMaterialUsage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftMaterialUsageRepository extends JpaRepository<ShiftMaterialUsage, Long> {
    // Strictly fetch usage only for a specific hub
    List<ShiftMaterialUsage> findByHub(Hub hub, Sort sort);
}