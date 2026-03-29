package com.example.NutriDeliver.service.IMS;

import com.example.NutriDeliver.dto.BatchRestockDTO;
import com.example.NutriDeliver.dto.PlannedUsageDTO;
import com.example.NutriDeliver.model.*;
import com.example.NutriDeliver.model.IMS.Inventory;
import com.example.NutriDeliver.model.IMS.InventoryTransactionLog;
import com.example.NutriDeliver.model.IMS.ShiftMaterialUsage;
import com.example.NutriDeliver.repository.*;
import com.example.NutriDeliver.repository.IMS.InventoryRepository;
import com.example.NutriDeliver.repository.IMS.InventoryTransactionLogRepository;
import com.example.NutriDeliver.repository.IMS.RecipeMappingRepository;
import com.example.NutriDeliver.repository.IMS.ShiftMaterialUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class InventoryService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private ShiftMaterialUsageRepository shiftUsageRepository;
    @Autowired private RecipeMappingRepository recipeMappingRepository;
    @Autowired private InventoryTransactionLogRepository transactionLogRepository;
    @Autowired private HubRepository hubRepository; // Needed to bind logs to Hub

    @Value("${app.inventory.strict-cutoffs:false}")
    private boolean strictCutoffsEnabled;

    private static final LocalTime BREAKFAST_CUTOFF = LocalTime.of(8, 0);
    private static final LocalTime LUNCH_CUTOFF = LocalTime.of(12, 0);
    private static final LocalTime DINNER_CUTOFF = LocalTime.of(19, 0);

    // Fetch stock for a SPECIFIC HUB ONLY
    public List<Inventory> getLiveStockForHub(Long hubId) {
        return inventoryRepository.findByHubId(hubId);
    }

    @Transactional
    public void receiveBatchRestock(BatchRestockDTO payload, String idempotencyKey, Long hubId) {
        if (payload.items == null || payload.items.isEmpty()) {
            throw new IllegalStateException("Invoice must contain at least one material row.");
        }

        Hub targetHub = hubRepository.findById(hubId).orElseThrow(() -> new IllegalStateException("Hub not found"));

        for (BatchRestockDTO.BatchRestockItem item : payload.items) {
            if (item.quantity == null || item.quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Quantity must be greater than 0");
            }

            String itemUniqueKey = idempotencyKey + "-HUB-" + hubId + "-MAT-" + item.materialId;

            if (transactionLogRepository.existsByIdempotencyKey(itemUniqueKey)) {
                continue;
            }

            // Find stock specifically for this Hub
            Inventory inv = inventoryRepository.findByHubIdAndMaterialId(hubId, item.materialId)
                    .orElseThrow(() -> new IllegalStateException("Material record missing for this Hub: " + item.materialId));

            BigDecimal stockBefore = inv.getCurrentStock();
            BigDecimal stockAfter = stockBefore.add(item.quantity);

            if (inv.getMaxCapacity() != null && inv.getMaxCapacity().compareTo(BigDecimal.ZERO) > 0) {
                if (stockAfter.compareTo(inv.getMaxCapacity()) > 0) {
                    BigDecimal allowedSpace = inv.getMaxCapacity().subtract(stockBefore);
                    throw new IllegalStateException("CAPACITY EXCEEDED for " + inv.getMaterial().getName() + "! " +
                            "Cannot add " + item.quantity + ". Only " + allowedSpace + " " + inv.getMaterial().getUnit() + " space remaining.");
                }
            }

            inv.setCurrentStock(stockAfter);
            inventoryRepository.save(inv);

            LocalDate parsedExpiry = null;
            if (item.expiryDate != null && !item.expiryDate.trim().isEmpty()) {
                parsedExpiry = LocalDate.parse(item.expiryDate);
            }

            InventoryTransactionLog log = new InventoryTransactionLog();
            log.setHub(targetHub); // Tie the invoice to the hub
            log.setMaterial(inv.getMaterial());
            log.setQuantityAdded(item.quantity);
            log.setStockBefore(stockBefore);
            log.setStockAfter(stockAfter);
            log.setSupplier(payload.supplier);
            log.setInvoiceNumber(payload.invoiceNumber);
            log.setReceivedBy(payload.receivedBy);
            log.setStorageLocation(item.storageLocation);
            log.setBatchNumber(item.batchNumber);
            log.setExpiryDate(parsedExpiry);
            log.setTransactionTime(LocalDateTime.now());
            log.setIdempotencyKey(itemUniqueKey);

            transactionLogRepository.save(log);
        }

        inventoryRepository.flush();
    }

    @Transactional
    public void issueStockToKitchen(Map<?, ?> issuanceData, MealOrder.MealType mealType, Long hubId) {
        if (issuanceData == null || issuanceData.isEmpty()) return;

        LocalTime now = LocalTime.now();
        LocalTime cutoff = switch (mealType) {
            case BREAKFAST -> BREAKFAST_CUTOFF;
            case LUNCH -> LUNCH_CUTOFF;
            case DINNER -> DINNER_CUTOFF;
            default -> throw new IllegalArgumentException("Invalid Meal Type.");
        };

        if (strictCutoffsEnabled && now.isAfter(cutoff)) {
            throw new IllegalStateException("FORBIDDEN: Window for " + mealType + " closed at " + cutoff);
        }

        for (Map.Entry<?, ?> entry : issuanceData.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;

            Long matId;
            try {
                matId = Long.parseLong(entry.getKey().toString());
            } catch (NumberFormatException e) {
                continue;
            }

            BigDecimal amount = safe(entry.getValue());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) continue;

            Inventory inv = inventoryRepository.findByHubIdAndMaterialId(hubId, matId)
                    .orElseThrow(() -> new IllegalStateException("Material record missing: " + matId));

            if (inv.getCurrentStock().compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient warehouse stock for " + inv.getMaterial().getName());
            }

            inv.setCurrentStock(inv.getCurrentStock().subtract(amount));
            inv.setKitchenStock(inv.getKitchenStock().add(amount));
            inventoryRepository.save(inv);
        }
        inventoryRepository.flush();
    }

    @Transactional
    public void reconcileAndReturn(Map<?, ?> actualUsage, Map<?, ?> wasteData, MealOrder.MealType mealType, Long hubId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        Hub targetHub = hubRepository.findById(hubId).orElseThrow(() -> new IllegalStateException("Hub not found"));

        Map<Long, BigDecimal> plannedMap = calculatePlannedUsageMap(start, end, mealType, OrderStatus.DELIVERED, hubId);

        List<Inventory> allHubInventory = inventoryRepository.findByHubId(hubId);

        for (Inventory inv : allHubInventory) {
            Long matId = inv.getMaterial().getId();

            BigDecimal issued = safe(inv.getKitchenStock());
            BigDecimal used = safe(getFromMap(actualUsage, matId));
            BigDecimal waste = safe(getFromMap(wasteData, matId));

            if (issued.compareTo(BigDecimal.ZERO) == 0 && used.compareTo(BigDecimal.ZERO) == 0 && waste.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal totalConsumed = used.add(waste);

            if (issued.compareTo(BigDecimal.ZERO) == 0 && totalConsumed.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalStateException("WORKFLOW ERROR: You are trying to log usage for " + inv.getMaterial().getName() + " but 0 was issued. Did you forget to click 'Issue to Kitchen' in Step 1?");
            } else if (issued.compareTo(totalConsumed) < 0) {
                throw new IllegalStateException("CRITICAL: Usage (" + totalConsumed + ") > Issued (" + issued + ") for " + inv.getMaterial().getName());
            }

            BigDecimal leftover = issued.subtract(totalConsumed);

            inv.setCurrentStock(inv.getCurrentStock().add(leftover));
            inv.setKitchenStock(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            inventoryRepository.save(inv);

            ShiftMaterialUsage log = new ShiftMaterialUsage();
            log.setHub(targetHub); // Tie the waste log to the Hub
            log.setReconciliationTime(LocalDateTime.now());
            log.setMealType(mealType);
            log.setMaterial(inv.getMaterial());
            log.setActualAmount(used);
            log.setWasteAmount(waste);
            BigDecimal planned = plannedMap.getOrDefault(matId, BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            log.setPlannedAmount(planned);
            log.setVariance(used.subtract(planned));
            shiftUsageRepository.save(log);
        }

        List<MealOrder> orders = orderRepository.findByOrderDateBetweenAndMealTypeAndStatus(start, end, mealType, OrderStatus.DELIVERED);
        orders.forEach(o -> o.setStatus(OrderStatus.RECONCILED));
        orderRepository.saveAll(orders);

        inventoryRepository.flush();
    }

    private Map<Long, BigDecimal> calculatePlannedUsageMap(LocalDateTime s, LocalDateTime e, MealOrder.MealType mt, OrderStatus status, Long hubId) {
        List<MealOrder> orders = orderRepository.findByOrderDateBetweenAndMealTypeAndStatus(s, e, mt, status);
        Map<Long, BigDecimal> accumulator = new HashMap<>();
        for (MealOrder order : orders) {
            if (order.getItems() == null) continue;
            for (String item : order.getItems().split(",")) {
                recipeMappingRepository.findByMealKeywordIgnoreCase(item.trim()).forEach(recipe -> {
                    Long id = recipe.getRawMaterial().getId();
                    BigDecimal yield = (recipe.getYieldFactor() != null && recipe.getYieldFactor().compareTo(BigDecimal.ZERO) > 0)
                            ? recipe.getYieldFactor() : BigDecimal.ONE;
                    BigDecimal required = recipe.getQuantityPerServing().divide(yield, 4, RoundingMode.HALF_UP);
                    accumulator.put(id, accumulator.getOrDefault(id, BigDecimal.ZERO).add(required));
                });
            }
        }
        return accumulator;
    }

    public List<PlannedUsageDTO> calculatePlannedUsage(LocalDateTime s, LocalDateTime e, MealOrder.MealType mt, OrderStatus status, Long hubId) {
        Map<Long, BigDecimal> map = calculatePlannedUsageMap(s, e, mt, status, hubId);
        List<PlannedUsageDTO> results = new ArrayList<>();
        map.forEach((id, amount) -> inventoryRepository.findByHubIdAndMaterialId(hubId, id).ifPresent(inv ->
                results.add(new PlannedUsageDTO(id, inv.getMaterial().getName(), inv.getMaterial().getUnit(), amount.doubleValue()))));
        return results;
    }

    private BigDecimal safe(Object val) {
        if (val == null) return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        if (val instanceof BigDecimal) return ((BigDecimal) val).setScale(4, RoundingMode.HALF_UP);
        try {
            return new BigDecimal(val.toString()).setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal getFromMap(Map<?, ?> map, Long key) {
        if (map == null) return BigDecimal.ZERO;
        Object val = map.get(key);
        if (val == null) val = map.get(String.valueOf(key));
        if (val == null) return BigDecimal.ZERO;
        return safe(val);
    }
}