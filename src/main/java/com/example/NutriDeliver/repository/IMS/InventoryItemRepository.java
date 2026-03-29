package com.example.NutriDeliver.repository.IMS;

import com.example.NutriDeliver.model.Hub;
import com.example.NutriDeliver.model.IMS.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    // Strictly fetch items only for a specific hub
    List<InventoryItem> findByHub(Hub hub);
    Optional<InventoryItem> findByHubAndItemName(Hub hub, String itemName);
}