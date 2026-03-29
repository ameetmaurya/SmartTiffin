package com.example.NutriDeliver.controller.IMS;

import com.example.NutriDeliver.dto.BatchRestockDTO;
import com.example.NutriDeliver.dto.PlannedUsageDTO;
import com.example.NutriDeliver.model.MealOrder;
import com.example.NutriDeliver.model.OrderStatus;
import com.example.NutriDeliver.model.IMS.Inventory;
import com.example.NutriDeliver.service.IMS.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryApiController {

    @Autowired
    private InventoryService inventoryService;

    // FIX: Now securely fetching by the Hub ID passed from the frontend UI dropdown
    @GetMapping("/warehouse/live")
    public ResponseEntity<List<Inventory>> getLiveWarehouseStock(@RequestParam("hubId") Long hubId) {
        return ResponseEntity.ok(inventoryService.getLiveStockForHub(hubId));
    }

    @GetMapping("/issuance/forecast")
    public ResponseEntity<?> getIssuanceForecast(@RequestParam("mealType") String mealTypeStr, @RequestParam("hubId") Long hubId) {
        try {
            if (mealTypeStr == null || mealTypeStr.trim().isEmpty() || mealTypeStr.equalsIgnoreCase("undefined")) {
                return ResponseEntity.badRequest().body("Please select a valid shift (Breakfast/Lunch/Dinner).");
            }
            MealOrder.MealType mealType = MealOrder.MealType.valueOf(mealTypeStr.toUpperCase());
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

            List<PlannedUsageDTO> forecast = inventoryService.calculatePlannedUsage(start, end, mealType, OrderStatus.PENDING, hubId);
            return ResponseEntity.ok(forecast);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid meal type: " + mealTypeStr);
        }
    }

    @PostMapping("/issuance/confirm")
    public ResponseEntity<?> confirmIssuance(
            @RequestParam("mealType") String mealTypeStr,
            @RequestParam("hubId") Long hubId,
            @RequestBody Map<Long, BigDecimal> issuanceData) {
        try {
            if (mealTypeStr == null || mealTypeStr.trim().isEmpty() || mealTypeStr.equalsIgnoreCase("undefined")) {
                return ResponseEntity.badRequest().body("Please select a valid shift before issuing stock.");
            }
            MealOrder.MealType mealType = MealOrder.MealType.valueOf(mealTypeStr.toUpperCase());
            inventoryService.issueStockToKitchen(issuanceData, mealType, hubId);
            return ResponseEntity.ok("Stock successfully issued to Kitchen WIP.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid meal type: " + mealTypeStr);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error during issuance.");
        }
    }

    @GetMapping("/reconciliation/forecast")
    public ResponseEntity<?> getReconciliationForecast(@RequestParam("mealType") String mealTypeStr, @RequestParam("hubId") Long hubId) {
        try {
            MealOrder.MealType mealType = MealOrder.MealType.valueOf(mealTypeStr.toUpperCase());
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

            List<PlannedUsageDTO> forecast = inventoryService.calculatePlannedUsage(start, end, mealType, OrderStatus.DELIVERED, hubId);
            return ResponseEntity.ok(forecast);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid meal type for reconciliation.");
        }
    }

    @PostMapping("/reconciliation/confirm")
    public ResponseEntity<?> confirmReconciliation(
            @RequestParam("mealType") String mealTypeStr,
            @RequestParam("hubId") Long hubId,
            @RequestBody Map<String, Map<Long, BigDecimal>> payload) {
        try {
            if (mealTypeStr == null || mealTypeStr.trim().isEmpty() || mealTypeStr.equalsIgnoreCase("undefined")) {
                return ResponseEntity.badRequest().body("Please select a valid shift.");
            }
            MealOrder.MealType mealType = MealOrder.MealType.valueOf(mealTypeStr.toUpperCase());

            Map<Long, BigDecimal> actualUsage = payload.get("usage");
            Map<Long, BigDecimal> wasteData = payload.get("waste");

            inventoryService.reconcileAndReturn(actualUsage, wasteData, mealType, hubId);
            return ResponseEntity.ok("Shift reconciled and stock returned successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid meal type.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error during reconciliation.");
        }
    }

    @PostMapping("/restock/batch")
    public ResponseEntity<?> receiveBatchRestock(
            @RequestParam("hubId") Long hubId,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody BatchRestockDTO payload) {
        try {
            inventoryService.receiveBatchRestock(payload, idempotencyKey, hubId);
            return ResponseEntity.ok("Batch restock successful. Invoice logged.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing batch restock.");
        }
    }
}