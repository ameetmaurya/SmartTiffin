package com.example.NutriDeliver.dto;

import java.math.BigDecimal;
import java.util.List;

public class BatchRestockDTO {
    public String supplier;
    public String invoiceNumber;
    public String receivedBy;
    public List<BatchRestockItem> items;

    public static class BatchRestockItem {
        public Long materialId;
        public BigDecimal quantity;
        public String storageLocation;
        public String batchNumber;
        public String expiryDate;
    }
}