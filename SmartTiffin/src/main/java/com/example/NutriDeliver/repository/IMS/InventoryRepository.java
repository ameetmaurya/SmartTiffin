package com.example.NutriDeliver.repository.IMS;

import com.example.NutriDeliver.model.IMS.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // FIX: All queries must now filter by Hub ID!
    List<Inventory> findByHubId(Long hubId);

    Optional<Inventory> findByHubIdAndMaterialId(Long hubId, Long materialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.hub.id = :hubId AND i.material.id = :materialId")
    Optional<Inventory> findByHubIdAndMaterialIdForUpdate(@Param("hubId") Long hubId, @Param("materialId") Long materialId);
}