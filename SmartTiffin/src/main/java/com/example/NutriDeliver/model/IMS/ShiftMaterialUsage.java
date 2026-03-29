package com.example.NutriDeliver.model.IMS;

import com.example.NutriDeliver.model.Hub;
import com.example.NutriDeliver.model.MealOrder;
import com.example.NutriDeliver.model.IMS.RawMaterial;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_material_usage")
public class ShiftMaterialUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    private Hub hub;

    private LocalDateTime reconciliationTime;

    // ───────────────────────────
    // FIELDS FOR MANUAL UI USAGE (Fixes InventoryController errors)
    // ───────────────────────────
    private String shiftType;
    private String itemName;
    private Double usedQuantity;
    private String unit;
    private Integer mealsProduced;

    // ───────────────────────────
    // FIELDS FOR ADVANCED RECONCILIATION (API)
    // ───────────────────────────
    @Enumerated(EnumType.STRING)
    private MealOrder.MealType mealType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private RawMaterial material;

    @Column(precision = 19, scale = 4)
    private BigDecimal plannedAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal actualAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal wasteAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal variance;

    // ───────────────────────────
    // GETTERS AND SETTERS
    // ───────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Hub getHub() { return hub; }
    public void setHub(Hub hub) { this.hub = hub; }

    public LocalDateTime getReconciliationTime() { return reconciliationTime; }
    public void setReconciliationTime(LocalDateTime reconciliationTime) { this.reconciliationTime = reconciliationTime; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Double getUsedQuantity() { return usedQuantity; }
    public void setUsedQuantity(Double usedQuantity) { this.usedQuantity = usedQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getMealsProduced() { return mealsProduced; }
    public void setMealsProduced(Integer mealsProduced) { this.mealsProduced = mealsProduced; }

    public MealOrder.MealType getMealType() { return mealType; }
    public void setMealType(MealOrder.MealType mealType) { this.mealType = mealType; }

    public RawMaterial getMaterial() { return material; }
    public void setMaterial(RawMaterial material) { this.material = material; }

    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public void setPlannedAmount(BigDecimal plannedAmount) { this.plannedAmount = plannedAmount; }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

    public BigDecimal getWasteAmount() { return wasteAmount; }
    public void setWasteAmount(BigDecimal wasteAmount) { this.wasteAmount = wasteAmount; }

    public BigDecimal getVariance() { return variance; }
    public void setVariance(BigDecimal variance) { this.variance = variance; }
}