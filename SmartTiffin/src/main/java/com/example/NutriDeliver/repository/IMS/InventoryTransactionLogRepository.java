package com.example.NutriDeliver.repository.IMS;

import com.example.NutriDeliver.model.Hub;
import com.example.NutriDeliver.model.IMS.InventoryTransactionLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionLogRepository extends JpaRepository<InventoryTransactionLog, Long> {

    // Fetch logs strictly for the selected Hub UI
    List<InventoryTransactionLog> findByHub(Hub hub, Sort sort);

    // Invoice duplication checks required by RestockService
    boolean existsByIdempotencyKey(String idempotencyKey);
    boolean existsByInvoiceNumber(String invoiceNumber);
}