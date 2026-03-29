package com.example.NutriDeliver.model.IMS;

import com.example.NutriDeliver.model.Hub;
import com.example.NutriDeliver.model.IMS.RawMaterial;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transaction_log")
public class InventoryTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    private Hub hub;

    private LocalDateTime transactionTime;

    // ───────────────────────────
    // FIELDS FOR MANUAL UI USAGE (Fixes InventoryController errors)
    // ───────────────────────────
    private String itemName;
    private String transactionType;
    private Double quantity;
    private String unit;
    private String remarks;
    private String performedBy;

    // ───────────────────────────
    // FIELDS FOR ADVANCED RESTOCK (API)
    // ───────────────────────────
    @Column(unique = true)
    private String idempotencyKey;

    private String receivedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private RawMaterial material;

    private String supplier;

    private String invoiceNumber;
    private String batchNumber;
    private LocalDate expiryDate;
    private String storageLocation;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantityAdded;

    @Column(precision = 10, scale = 2)
    private BigDecimal stockBefore;

    @Column(precision = 10, scale = 2)
    private BigDecimal stockAfter;

    // ───────────────────────────
    // GETTERS AND SETTERS
    // ───────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Hub getHub() { return hub; }
    public void setHub(Hub hub) { this.hub = hub; }

    public LocalDateTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }

    public RawMaterial getMaterial() { return material; }
    public void setMaterial(RawMaterial material) { this.material = material; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }

    public BigDecimal getQuantityAdded() { return quantityAdded; }
    public void setQuantityAdded(BigDecimal quantityAdded) { this.quantityAdded = quantityAdded; }

    public BigDecimal getStockBefore() { return stockBefore; }
    public void setStockBefore(BigDecimal stockBefore) { this.stockBefore = stockBefore; }

    public BigDecimal getStockAfter() { return stockAfter; }
    public void setStockAfter(BigDecimal stockAfter) { this.stockAfter = stockAfter; }
}