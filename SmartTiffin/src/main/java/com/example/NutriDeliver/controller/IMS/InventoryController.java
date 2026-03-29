package com.example.NutriDeliver.controller.IMS;

import com.example.NutriDeliver.model.AdminRole;
import com.example.NutriDeliver.model.Hub;
import com.example.NutriDeliver.model.IMS.InventoryItem;
import com.example.NutriDeliver.model.IMS.InventoryTransactionLog;
import com.example.NutriDeliver.model.IMS.ShiftMaterialUsage;
import com.example.NutriDeliver.model.User;
import com.example.NutriDeliver.repository.HubRepository;
import com.example.NutriDeliver.repository.UserRepository;
import com.example.NutriDeliver.repository.IMS.InventoryItemRepository;
import com.example.NutriDeliver.repository.IMS.InventoryTransactionLogRepository;
import com.example.NutriDeliver.repository.IMS.ShiftMaterialUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class InventoryController {

    @Autowired private UserRepository userRepo;
    @Autowired private HubRepository hubRepo;
    @Autowired private InventoryItemRepository itemRepo;
    @Autowired private InventoryTransactionLogRepository logRepo;
    @Autowired private ShiftMaterialUsageRepository usageRepo;

    /**
     * CORE SECURITY FUNCTION: Resolves which hub the user is allowed to look at.
     * Hub Managers are FORCED to use their assigned hub.
     * Super Admins can select any hub via the dropdown (requestedHubId).
     */
    private Hub resolveTargetHub(User admin, Long requestedHubId) {
        if (admin.getAdminRole() == AdminRole.HUB_MANAGER && admin.getAssignedHub() != null) {
            return admin.getAssignedHub(); // Lockdown
        }

        if (requestedHubId != null) {
            return hubRepo.findById(requestedHubId).orElse(hubRepo.findByActiveTrue().stream().findFirst().orElse(null));
        }

        // Default to the first active hub
        return hubRepo.findByActiveTrue().stream().findFirst().orElse(null);
    }

    // ─────────────────────────── INVENTORY PAGE ─────────────────────────── //

    @GetMapping("/inventory")
    public String showInventory(@RequestParam(required = false) Long hubId, Principal principal, Model model) {
        User admin = userRepo.findByEmail(principal.getName());
        Hub targetHub = resolveTargetHub(admin, hubId);

        if (targetHub == null) {
            return "redirect:/admin/dashboard?error=NoActiveHubsAvailable";
        }

        // Add common layout variables
        model.addAttribute("adminUser", admin);
        model.addAttribute("adminRole", admin.getAdminRole() != null ? admin.getAdminRole().name() : "SUPER_ADMIN");
        model.addAttribute("isSuperAdmin", admin.getAdminRole() == null || admin.getAdminRole() == AdminRole.SUPER_ADMIN);
        model.addAttribute("isHubManager", admin.getAdminRole() == AdminRole.HUB_MANAGER);
        model.addAttribute("isViewer", admin.getAdminRole() == AdminRole.VIEWER);

        // Add hub specific data
        model.addAttribute("allHubs", hubRepo.findByActiveTrue());
        model.addAttribute("selectedHub", targetHub);

        // 🚨 STRICT FILTER: Fetch only items belonging to the targetHub!
        List<InventoryItem> items = itemRepo.findByHub(targetHub);
        model.addAttribute("inventoryItems", items);

        return "admin/inventory";
    }

    // ─────────────────────────── REPORTS PAGE ─────────────────────────── //

    @GetMapping("/reports")
    public String showReports(@RequestParam(required = false) Long hubId, Principal principal, Model model) {
        User admin = userRepo.findByEmail(principal.getName());
        Hub targetHub = resolveTargetHub(admin, hubId);

        if (targetHub == null) {
            return "redirect:/admin/dashboard?error=NoActiveHubsAvailable";
        }

        model.addAttribute("adminUser", admin);
        model.addAttribute("adminRole", admin.getAdminRole() != null ? admin.getAdminRole().name() : "SUPER_ADMIN");
        model.addAttribute("isSuperAdmin", admin.getAdminRole() == null || admin.getAdminRole() == AdminRole.SUPER_ADMIN);
        model.addAttribute("isHubManager", admin.getAdminRole() == AdminRole.HUB_MANAGER);
        model.addAttribute("isViewer", admin.getAdminRole() == AdminRole.VIEWER);

        model.addAttribute("allHubs", hubRepo.findByActiveTrue());
        model.addAttribute("selectedHub", targetHub);

        // 🚨 STRICT FILTER: Fetch logs only for the selected hub!
        List<InventoryTransactionLog> logs = logRepo.findByHub(targetHub, Sort.by(Sort.Direction.DESC, "transactionTime"));
        List<ShiftMaterialUsage> usages = usageRepo.findByHub(targetHub, Sort.by(Sort.Direction.DESC, "reconciliationTime"));

        model.addAttribute("logs", logs);
        model.addAttribute("usages", usages);

        return "admin/reports";
    }

    // ─────────────────────────── ADD / RESTOCK ITEM ─────────────────────────── //

    @PostMapping("/inventory/restock")
    public String restockItem(
            @RequestParam Long targetHubId, // Pass the hub context from the form
            @RequestParam String itemName,
            @RequestParam String category,
            @RequestParam Double quantity,
            @RequestParam String unit,
            @RequestParam Double minThreshold,
            @RequestParam Double unitPrice,
            Principal principal) {

        User admin = userRepo.findByEmail(principal.getName());
        if (admin.getAdminRole() == AdminRole.VIEWER) return "redirect:/admin/inventory?error=AccessDenied";

        Hub targetHub = resolveTargetHub(admin, targetHubId);

        // Find existing item IN THIS SPECIFIC HUB, or create new
        InventoryItem item = itemRepo.findByHubAndItemName(targetHub, itemName).orElse(new InventoryItem());
        item.setHub(targetHub);
        item.setItemName(itemName);
        item.setCategory(category);
        item.setCurrentStock((item.getCurrentStock() == null ? 0 : item.getCurrentStock()) + quantity);
        item.setUnit(unit);
        item.setMinThreshold(minThreshold);
        item.setUnitPrice(unitPrice);
        itemRepo.save(item);

        // Log transaction
        InventoryTransactionLog log = new InventoryTransactionLog();
        log.setHub(targetHub);
        log.setItemName(itemName);
        log.setTransactionType("IN");
        log.setQuantity(quantity);
        log.setUnit(unit);
        log.setRemarks("Restock by " + admin.getFullName());
        log.setTransactionTime(LocalDateTime.now());
        log.setPerformedBy(admin.getFullName());
        logRepo.save(log);

        return "redirect:/admin/inventory?hubId=" + targetHub.getId() + "&msg=ItemRestocked";
    }

    // ─────────────────────────── DEDUCT / USE ITEM ─────────────────────────── //

    @PostMapping("/inventory/deduct")
    public String deductItem(
            @RequestParam Long targetHubId,
            @RequestParam String itemName,
            @RequestParam Double usedQuantity,
            @RequestParam String shiftType,
            @RequestParam Integer mealsProduced,
            Principal principal) {

        User admin = userRepo.findByEmail(principal.getName());
        if (admin.getAdminRole() == AdminRole.VIEWER) return "redirect:/admin/inventory?error=AccessDenied";

        Hub targetHub = resolveTargetHub(admin, targetHubId);
        InventoryItem item = itemRepo.findByHubAndItemName(targetHub, itemName).orElse(null);

        if (item == null || item.getCurrentStock() < usedQuantity) {
            return "redirect:/admin/inventory?hubId=" + targetHub.getId() + "&error=InsufficientStock";
        }

        item.setCurrentStock(item.getCurrentStock() - usedQuantity);
        itemRepo.save(item);

        // Record Usage
        ShiftMaterialUsage usage = new ShiftMaterialUsage();
        usage.setHub(targetHub);
        usage.setShiftType(shiftType);
        usage.setItemName(itemName);
        usage.setUsedQuantity(usedQuantity);
        usage.setUnit(item.getUnit());
        usage.setMealsProduced(mealsProduced);
        usage.setReconciliationTime(LocalDateTime.now());
        usageRepo.save(usage);

        // Log transaction
        InventoryTransactionLog log = new InventoryTransactionLog();
        log.setHub(targetHub);
        log.setItemName(itemName);
        log.setTransactionType("OUT");
        log.setQuantity(usedQuantity);
        log.setUnit(item.getUnit());
        log.setRemarks("Usage for " + shiftType);
        log.setTransactionTime(LocalDateTime.now());
        log.setPerformedBy(admin.getFullName());
        logRepo.save(log);

        return "redirect:/admin/inventory?hubId=" + targetHub.getId() + "&msg=StockDeducted";
    }
}