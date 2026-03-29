package com.example.NutriDeliver.dto;

public class PlannedUsageDTO {
    private Long materialId;
    private String materialName;
    private String unit;
    private Double predictedAmount;

    public PlannedUsageDTO(Long materialId, String materialName, String unit, Double predictedAmount) {
        this.materialId = materialId;
        this.materialName = materialName;
        this.unit = unit;
        this.predictedAmount = predictedAmount;
    }

    public Double getPredictedAmount() { return predictedAmount; }
    public void setPredictedAmount(Double predictedAmount) { this.predictedAmount = predictedAmount; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}