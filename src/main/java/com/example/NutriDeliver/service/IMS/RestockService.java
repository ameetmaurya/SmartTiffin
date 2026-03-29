package com.example.NutriDeliver.service.IMS;

import com.example.NutriDeliver.dto.RestockRequestDTO;
import com.example.NutriDeliver.model.Hub;
import com.example.NutriDeliver.model.IMS.Inventory;
import com.example.NutriDeliver.model.IMS.InventoryTransactionLog;
import com.example.NutriDeliver.model.IMS.RawMaterial;
import com.example.NutriDeliver.repository.HubRepository;
import com.example.NutriDeliver.repository.IMS.InventoryRepository;
import com.example.NutriDeliver.repository.IMS.InventoryTransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class RestockService {

    @Autowired
    private InventoryRepository inventoryRepo;

    @Autowired
    private InventoryTransactionLogRepository auditRepo;

    @Autowired
    private HubRepository hubRepo; // FIX: Added HubRepository to tie invoices to specific hubs

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void processRestock(RestockRequestDTO request, String idempotencyKey, String username, Long hubId) {
        if (auditRepo.existsByIdempotencyKey(idempotencyKey)) { return; }
        if (auditRepo.existsByInvoiceNumber(request.getInvoiceNumber())) {
            throw new IllegalStateException("Duplicate Invoice Number detected.");
        }

        Hub targetHub = hubRepo.findById(hubId)
                .orElseThrow(() -> new IllegalStateException("Hub not found."));

        // FIX: Now filtering inventory using BOTH Hub ID and Material ID
        Inventory inventory = inventoryRepo.findByHubIdAndMaterialId(hubId, request.getMaterialId())
                .orElseThrow(() -> new IllegalArgumentException("Material not found in warehouse inventory for this hub."));

        RawMaterial material = inventory.getMaterial();
        BigDecimal qtyToAdd = request.getQuantity();

        if (qtyToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }

        BigDecimal newStock = inventory.getCurrentStock().add(qtyToAdd);

        if (inventory.getMaxCapacity() != null && newStock.compareTo(inventory.getMaxCapacity()) > 0) {
            throw new IllegalStateException("Restock exceeds maximum warehouse capacity.");
        }

        String matName = material.getName().toLowerCase();
        if (matName.contains("paneer") && request.getStorageLocation().equals("DRY_STORE")) {
            throw new IllegalArgumentException("Food Safety Violation: Paneer cannot be stored in Dry Store.");
        }
        if (matName.contains("oil") && request.getStorageLocation().equals("FREEZER")) {
            throw new IllegalArgumentException("Quality Violation: Oil cannot be stored in Freezer.");
        }

        if (request.getExpiryDate() != null && request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot receive expired goods.");
        }

        InventoryTransactionLog log = new InventoryTransactionLog();
        log.setHub(targetHub); // FIX: Ensure transaction log is bound to the specific Hub
        log.setTransactionTime(LocalDateTime.now());
        log.setIdempotencyKey(idempotencyKey);
        log.setReceivedBy(username);
        log.setMaterial(material);
        log.setSupplier(request.getSupplier());
        log.setInvoiceNumber(request.getInvoiceNumber());
        log.setBatchNumber(request.getBatchNumber());
        log.setExpiryDate(request.getExpiryDate());
        log.setStorageLocation(request.getStorageLocation());
        log.setQuantityAdded(qtyToAdd);
        log.setStockBefore(inventory.getCurrentStock());
        log.setStockAfter(newStock);

        auditRepo.save(log);

        inventory.setCurrentStock(newStock);
        inventoryRepo.save(inventory);
    }
}