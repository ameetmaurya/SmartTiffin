package com.example.NutriDeliver.model.IMS;

import com.example.NutriDeliver.model.Hub;
import com.example.NutriDeliver.model.IMS.RawMaterial;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FIX: Tells Jackson to completely ignore this lazy-loaded proxy when generating JSON
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    // FIX: Tells Jackson to ignore the Hibernate proxy wrapper but keep the actual Material data
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private RawMaterial material;

    @Column(nullable = false, precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.0000")
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.0000")
    private BigDecimal kitchenStock = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal maxCapacity;

    @Column(precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal alertThreshold;

    @Version
    private Long version = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Hub getHub() { return hub; }
    public void setHub(Hub hub) { this.hub = hub; }

    public RawMaterial getMaterial() { return material; }
    public void setMaterial(RawMaterial material) { this.material = material; }

    public BigDecimal getCurrentStock() { return currentStock; }
    public void setCurrentStock(BigDecimal currentStock) { this.currentStock = currentStock; }

    public BigDecimal getKitchenStock() { return kitchenStock; }
    public void setKitchenStock(BigDecimal kitchenStock) { this.kitchenStock = kitchenStock; }

    public BigDecimal getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(BigDecimal maxCapacity) { this.maxCapacity = maxCapacity; }

    public BigDecimal getAlertThreshold() { return alertThreshold; }
    public void setAlertThreshold(BigDecimal alertThreshold) { this.alertThreshold = alertThreshold; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}