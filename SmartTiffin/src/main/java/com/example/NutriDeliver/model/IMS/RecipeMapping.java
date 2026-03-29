package com.example.NutriDeliver.model.IMS;

import com.example.NutriDeliver.model.IMS.RawMaterial;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recipe_mapping")
public class RecipeMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mealKeyword;

    @ManyToOne
    @JoinColumn(name = "raw_material_id")
    private RawMaterial rawMaterial;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityPerServing;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal yieldFactor = BigDecimal.ONE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMealKeyword() { return mealKeyword; }
    public void setMealKeyword(String mealKeyword) { this.mealKeyword = mealKeyword; }
    public RawMaterial getRawMaterial() { return rawMaterial; }
    public void setRawMaterial(RawMaterial rawMaterial) { this.rawMaterial = rawMaterial; }
    public BigDecimal getQuantityPerServing() { return quantityPerServing; }
    public void setQuantityPerServing(BigDecimal quantityPerServing) { this.quantityPerServing = quantityPerServing; }
    public BigDecimal getYieldFactor() { return yieldFactor; }
    public void setYieldFactor(BigDecimal yieldFactor) { this.yieldFactor = yieldFactor; }
}