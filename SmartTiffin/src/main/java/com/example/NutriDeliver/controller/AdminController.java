package com.example.NutriDeliver.controller;

import com.example.NutriDeliver.model.*;
import com.example.NutriDeliver.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private OrderRepository      orderRepo;
    @Autowired private ShiftRepository      shiftRepo;
    @Autowired private WeeklyMenuRepository weeklyMenuRepo;
    @Autowired private UserRepository       userRepo;
    @Autowired private HubRepository        hubRepo;
    @Autowired private MenuPlanRepository   menuPlanRepo;
    @Autowired private VerificationDocumentRepository verDocRepo;

    // ─────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────

    private MealOrder.MealType getCurrentKitchenShift() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(10, 30))) return MealOrder.MealType.BREAKFAST;
        if (now.isBefore(LocalTime.of(15, 30))) return MealOrder.MealType.LUNCH;
        return MealOrder.MealType.DINNER;
    }

    private User getAdmin(Principal principal) {
        if (principal == null) throw new IllegalStateException("No authenticated user");
        User u = userRepo.findByEmail(principal.getName());
        if (u == null) throw new IllegalStateException("Admin user not found: " + principal.getName());
        return u;
    }

    private AdminRole getAdminRole(User admin) {
        return (admin.getAdminRole() != null) ? admin.getAdminRole() : AdminRole.SUPER_ADMIN;
    }

    private String writeGuard(User admin) {
        if (getAdminRole(admin) == AdminRole.VIEWER)
            return "redirect:/admin/dashboard?error=AccessDenied";
        return null;
    }

    private String superAdminGuard(User admin) {
        if (getAdminRole(admin) != AdminRole.SUPER_ADMIN)
            return "redirect:/admin/dashboard?error=AccessDenied";
        return null;
    }

    private boolean hasStatus(User u, VerificationStatus target) {
        return u.getVerificationStatus() != null && u.getVerificationStatus() == target;
    }

    private boolean hasRole(User u, String roleName) {
        return u.getRole() != null && roleName.equals(u.getRole().name());
    }

    private void populateCommonModel(Model model, User admin) {
        AdminRole role = getAdminRole(admin);
        model.addAttribute("adminRole",    role.name());
        model.addAttribute("isSuperAdmin", role == AdminRole.SUPER_ADMIN);
        model.addAttribute("isHubManager", role == AdminRole.HUB_MANAGER);
        model.addAttribute("isViewer",     role == AdminRole.VIEWER);
        model.addAttribute("adminUser",    admin);

        if (role == AdminRole.HUB_MANAGER && admin.getAssignedHub() != null) {
            model.addAttribute("allHubs", List.of(admin.getAssignedHub()));
        } else {
            model.addAttribute("allHubs", hubRepo.findAll());
        }

        List<MenuPlan> plans = menuPlanRepo.findAll();
        model.addAttribute("allPlans",   plans);
        model.addAttribute("activePlan", menuPlanRepo.findByActiveTrue().orElse(null));

        long pending = userRepo.countByRoleAndVerificationStatus(User.Role.COOK, VerificationStatus.PENDING) +
                       userRepo.countByRoleAndVerificationStatus(User.Role.DELIVERY_PARTNER, VerificationStatus.PENDING);
        model.addAttribute("pendingVerifications", pending);
    }

    // ─────────────────────────────────────────
    //  DASHBOARD
    // ─────────────────────────────────────────

    @GetMapping("/dashboard")
    public String adminDashboard(Principal principal, Model model) {
        User admin = getAdmin(principal);
        AdminRole role = getAdminRole(admin);
        populateCommonModel(model, admin);

        model.addAttribute("totalStudents", userRepo.countByRole(User.Role.STUDENT));
        model.addAttribute("totalCooks",    userRepo.countByRole(User.Role.COOK));
        model.addAttribute("totalDrivers",  userRepo.countByRole(User.Role.DELIVERY_PARTNER));

        MealOrder.MealType currentShift = getCurrentKitchenShift();
        model.addAttribute("currentShift", currentShift.name());

        List<MealOrder> activeOrders = orderRepo.findAll().stream()
                .filter(o -> o.getOrderDate() != null
                        && o.getOrderDate().toLocalDate().equals(LocalDate.now()))
                .filter(o -> o.getMealType() == currentShift)
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());
        model.addAttribute("activeOrders", activeOrders);

        List<ShiftBooking> todayShifts = shiftRepo.findAll().stream()
                .filter(b -> b.getShiftDate() != null
                        && !b.getShiftDate().isBefore(LocalDate.now()))
                .filter(b -> {
                    if (role == AdminRole.HUB_MANAGER && admin.getAssignedHub() != null) {
                        return b.getHub() != null
                                && b.getHub().getId().equals(admin.getAssignedHub().getId());
                    }
                    return true;
                })
                .sorted(Comparator.comparing(ShiftBooking::getShiftDate))
                .collect(Collectors.toList());
        model.addAttribute("todayShifts", todayShifts);

        model.addAttribute("predictedOrders", activeOrders.size() + 5);
        model.addAttribute("vegKgNeeded",     (activeOrders.size() + 5) / 2);

        MenuPlan activePlan = menuPlanRepo.findByActiveTrue().orElse(null);
        List<WeeklyMenu> fullMenu = (activePlan != null)
                ? weeklyMenuRepo.findByMenuPlan(activePlan)
                : weeklyMenuRepo.findAll();

        model.addAttribute("vegMenu",
                fullMenu.stream()
                        .filter(m -> "Pure Veg".equals(m.getMealCategory()))
                        .collect(Collectors.toList()));
        model.addAttribute("nonVegMenu",
                fullMenu.stream()
                        .filter(m -> "Non-Veg".equals(m.getMealCategory()))
                        .collect(Collectors.toList()));

        return "admin/dashboard";
    }

    @GetMapping("/force-pack-orders")
    public String simulateKitchenDone() {
        List<MealOrder> pendingOrders = orderRepo.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .collect(Collectors.toList());
        for (MealOrder order : pendingOrders) {
            order.setStatus(OrderStatus.PACKED);
        }
        orderRepo.saveAll(pendingOrders);
        return "redirect:/admin/dashboard?msg=Kitchen+Simulation+Complete.+Orders+are+now+PACKED!";
    }

    // ─────────────────────────────────────────
    //  HUBS
    // ─────────────────────────────────────────

    @GetMapping("/hubs")
    public String listHubs(Principal principal, Model model) {
        User admin = getAdmin(principal);
        populateCommonModel(model, admin);
        model.addAttribute("hubs",
                (getAdminRole(admin) == AdminRole.HUB_MANAGER && admin.getAssignedHub() != null)
                        ? List.of(admin.getAssignedHub())
                        : hubRepo.findAll());
        return "admin/hubs";
    }

    @PostMapping("/hubs/create")
    public String createHub(
            Principal principal,
            @RequestParam String name,
            @RequestParam String address,
            @RequestParam String area,
            @RequestParam String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "10") int capacity) {

        String block = superAdminGuard(getAdmin(principal));
        if (block != null) return block;

        Hub hub = new Hub();
        hub.setName(name);
        hub.setAddress(address);
        hub.setArea(area);
        hub.setCity(city);
        hub.setLatitude(latitude);
        hub.setLongitude(longitude);
        hub.setCapacity(capacity);
        hub.setActive(true);
        hubRepo.save(hub);
        return "redirect:/admin/hubs?msg=HubCreated";
    }

    @PostMapping("/hubs/toggle/{id}")
    public String toggleHub(@PathVariable Long id, Principal principal) {
        User admin = getAdmin(principal);
        String block = writeGuard(admin);
        if (block != null) return block;

        if (getAdminRole(admin) == AdminRole.HUB_MANAGER) {
            if (admin.getAssignedHub() == null || !admin.getAssignedHub().getId().equals(id))
                return "redirect:/admin/hubs?error=AccessDenied";
        }
        Hub hub = hubRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hub not found: " + id));
        hub.setActive(!hub.isActive());
        hubRepo.save(hub);
        return "redirect:/admin/hubs?msg=HubUpdated";
    }

    @PostMapping("/hubs/delete/{id}")
    public String deleteHub(@PathVariable Long id, Principal principal) {
        String block = superAdminGuard(getAdmin(principal));
        if (block != null) return block;

        boolean hasBookings = shiftRepo.findAll().stream()
                .anyMatch(b -> b.getHub() != null
                        && b.getHub().getId().equals(id)
                        && b.getShiftDate() != null
                        && !b.getShiftDate().isBefore(LocalDate.now()));
        if (hasBookings) return "redirect:/admin/hubs?error=HubHasActiveBookings";

        hubRepo.deleteById(id);
        return "redirect:/admin/hubs?msg=HubDeleted";
    }

    // ─────────────────────────────────────────
    //  MEAL PLANS
    // ─────────────────────────────────────────

    @GetMapping("/plans")
    public String listPlans(Principal principal, Model model) {
        User admin = getAdmin(principal);
        populateCommonModel(model, admin);
        model.addAttribute("plans", menuPlanRepo.findAll());

        MenuPlan activePlan = menuPlanRepo.findByActiveTrue().orElse(null);
        List<WeeklyMenu> fullMenu = (activePlan != null)
                ? weeklyMenuRepo.findByMenuPlan(activePlan)
                : new ArrayList<>();

        model.addAttribute("vegMenu",
                fullMenu.stream()
                        .filter(m -> "Pure Veg".equals(m.getMealCategory()))
                        .collect(Collectors.toList()));
        model.addAttribute("nonVegMenu",
                fullMenu.stream()
                        .filter(m -> "Non-Veg".equals(m.getMealCategory()))
                        .collect(Collectors.toList()));

        return "admin/plans";
    }

    @PostMapping("/plans/create")
    public String createPlan(
            Principal principal,
            @RequestParam String name,
            @RequestParam(required = false) String description) {

        String block = superAdminGuard(getAdmin(principal));
        if (block != null) return block;

        MenuPlan plan = new MenuPlan();
        plan.setName(name);
        plan.setDescription(description);
        plan.setActive(false);
        menuPlanRepo.save(plan);
        return "redirect:/admin/plans?msg=PlanCreated";
    }

    @PostMapping("/plans/activate/{id}")
    public String activatePlan(@PathVariable Long id, Principal principal) {
        String block = superAdminGuard(getAdmin(principal));
        if (block != null) return block;

        menuPlanRepo.findAll().forEach(p -> {
            p.setActive(false);
            menuPlanRepo.save(p);
        });
        MenuPlan plan = menuPlanRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
        plan.setActive(true);
        menuPlanRepo.save(plan);
        return "redirect:/admin/plans?msg=PlanActivated";
    }

    @PostMapping("/plans/delete/{id}")
    public String deletePlan(@PathVariable Long id, Principal principal) {
        String block = superAdminGuard(getAdmin(principal));
        if (block != null) return block;

        MenuPlan plan = menuPlanRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
        weeklyMenuRepo.deleteByMenuPlan(plan);
        menuPlanRepo.delete(plan);
        return "redirect:/admin/plans?msg=PlanDeleted";
    }

    // ─────────────────────────────────────────
    //  MENU UPDATE
    // ─────────────────────────────────────────

    @PostMapping("/menu/update")
    public String updateMenu(
            Principal principal,
            @RequestParam Long planId,
            @RequestParam String dayOfWeek,
            @RequestParam String mealType,
            @RequestParam String mealCategory,
            @RequestParam String items,
            @RequestParam int calories,
            @RequestParam int protein,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        String block = writeGuard(getAdmin(principal));
        if (block != null) return block;

        MenuPlan plan = menuPlanRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        DayOfWeek day = DayOfWeek.valueOf(dayOfWeek);
        MealOrder.MealType slot = MealOrder.MealType.valueOf(mealType);

        WeeklyMenu menu = weeklyMenuRepo
                .findByMenuPlanAndDayOfWeekAndMealTypeAndMealCategory(plan, day, slot, mealCategory)
                .orElse(new WeeklyMenu());

        menu.setMenuPlan(plan);
        menu.setDayOfWeek(day);
        menu.setMealType(slot);
        menu.setMealCategory(mealCategory);
        menu.setItems(items);
        menu.setCalories(calories);
        menu.setProtein(protein);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String originalName = imageFile.getOriginalFilename();
                String safeName = (originalName != null)
                        ? originalName.replaceAll("[^a-zA-Z0-9._\\-]", "_") : "upload";
                String uploadDir = "uploads/";
                new File(uploadDir).mkdirs();
                String fileName = UUID.randomUUID() + "_" + safeName;
                Files.copy(imageFile.getInputStream(),
                        Paths.get(uploadDir + fileName),
                        StandardCopyOption.REPLACE_EXISTING);
                menu.setImageUrl("/uploads/" + fileName);
            } catch (IOException e) {
                return "redirect:/admin/plans?error=FileUploadFailed";
            }
        }

        weeklyMenuRepo.save(menu);
        return "redirect:/admin/plans?msg=MenuUpdated&planId=" + planId;
    }

    // ─────────────────────────────────────────
    //  ORDERS
    // ─────────────────────────────────────────

    @GetMapping("/orders")
    public String listOrders(
            Principal principal,
            Model model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String slot,
            @RequestParam(required = false) String category) {

        User admin = getAdmin(principal);
        populateCommonModel(model, admin);

        List<MealOrder> orders = orderRepo.findAll();

        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().name().equals(status))
                    .collect(Collectors.toList());
        }
        if (slot != null && !slot.isBlank()) {
            orders = orders.stream()
                    .filter(o -> o.getMealType() != null && o.getMealType().name().equals(slot))
                    .collect(Collectors.toList());
        }
        if (category != null && !category.isBlank()) {
            orders = orders.stream()
                    .filter(o -> category.equals(o.getMealCategory()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("orders", orders);

        long totalToday = orderRepo.findAll().stream()
                .filter(o -> o.getOrderDate() != null
                        && o.getOrderDate().toLocalDate().equals(LocalDate.now()))
                .count();
        model.addAttribute("totalToday",     totalToday);
        model.addAttribute("countCooking",   countByStatus(orders, OrderStatus.PENDING));
        model.addAttribute("countPacked",    countByStatus(orders, OrderStatus.PACKED));
        model.addAttribute("countOut",       countByStatus(orders, OrderStatus.OUT_FOR_DELIVERY));
        model.addAttribute("countDelivered", countByStatus(orders, OrderStatus.DELIVERED));

        return "admin/orders";
    }

    private long countByStatus(List<MealOrder> list, OrderStatus s) {
        return list.stream().filter(o -> o.getStatus() == s).count();
    }

    // ─────────────────────────────────────────
    //  STUDENTS (OPTIMIZED)
    // ─────────────────────────────────────────

    @GetMapping("/students")
    public String listStudents(Principal principal, Model model) {
        User admin = getAdmin(principal);
        populateCommonModel(model, admin);

        List<User> students = userRepo.findByRole(User.Role.STUDENT);

        model.addAttribute("students", students);
        model.addAttribute("totalStudents", userRepo.countByRole(User.Role.STUDENT));
        model.addAttribute("activeSubscriptions", userRepo.countActiveSubscriptions());
        model.addAttribute("blockA", userRepo.countStudentsByBlock("Block A (Boys)"));

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        model.addAttribute("newThisWeek", userRepo.countNewStudentsSince(weekAgo));

        return "admin/students";
    }

    // ─────────────────────────────────────────
    //  COOKS (OPTIMIZED)
    // ─────────────────────────────────────────

    @GetMapping("/cooks")
    public String listCooks(Principal principal, Model model) {
        User admin = getAdmin(principal);
        populateCommonModel(model, admin);

        List<User> cooks = userRepo.findByRole(User.Role.COOK);

        model.addAttribute("cooks", cooks);
        model.addAttribute("totalCooks", userRepo.countByRole(User.Role.COOK));
        model.addAttribute("approvedCooks", userRepo.countByRoleAndVerificationStatus(User.Role.COOK, VerificationStatus.APPROVED));
        model.addAttribute("pendingCooks",  userRepo.countByRoleAndVerificationStatus(User.Role.COOK, VerificationStatus.PENDING));

        return "admin/cooks";
    }

    // ─────────────────────────────────────────
    //  RIDERS (OPTIMIZED)
    // ─────────────────────────────────────────

    @GetMapping("/riders")
    public String listRiders(Principal principal, Model model) {
        User admin = getAdmin(principal);
        populateCommonModel(model, admin);

        List<User> riders = userRepo.findByRole(User.Role.DELIVERY_PARTNER);

        model.addAttribute("riders", riders);
        model.addAttribute("totalRiders", userRepo.countByRole(User.Role.DELIVERY_PARTNER));
        model.addAttribute("verifiedRiders", userRepo.countByRoleAndVerificationStatus(User.Role.DELIVERY_PARTNER, VerificationStatus.APPROVED));
        model.addAttribute("pendingRiders",  userRepo.countByRoleAndVerificationStatus(User.Role.DELIVERY_PARTNER, VerificationStatus.PENDING));

        List<Long> onDutyIds = shiftRepo.findAll().stream()
                .filter(b -> b.getShiftDate() != null
                        && b.getShiftDate().equals(LocalDate.now())
                        && b.isCheckedIn()
                        && b.getDriver() != null)
                .map(b -> b.getDriver().getId())
                .collect(Collectors.toList());
        model.addAttribute("onDutyToday", (long) onDutyIds.size());

        return "admin/riders";
    }

    // ─────────────────────────────────────────
    //  ORDER STATUS UPDATE
    // ─────────────────────────────────────────

    @PostMapping("/order/update/{id}")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam("status") String status,
            Principal principal) {

        String block = writeGuard(getAdmin(principal));
        if (block != null) return block;

        MealOrder order = orderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        try {
            order.setStatus(OrderStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/orders?error=InvalidStatus";
        }
        orderRepo.save(order);
        return "redirect:/admin/orders?msg=StatusUpdated";
    }

    // ─────────────────────────────────────────
    //  SHIFT MANUAL CHECK-IN
    // ─────────────────────────────────────────

    @PostMapping("/shift/manual-checkin/{id}")
    public String manualCheckin(@PathVariable Long id, Principal principal) {
        String block = writeGuard(getAdmin(principal));
        if (block != null) return block;

        ShiftBooking booking = shiftRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + id));
        booking.setCheckedIn(true);
        shiftRepo.save(booking);
        return "redirect:/admin/dashboard?msg=DriverCheckedInManually";
    }

    // ─────────────────────────────────────────
    //  ADMIN TEAM
    // ─────────────────────────────────────────

    @GetMapping("/team")
    public String listAdmins(Principal principal, Model model) {
        User admin = getAdmin(principal);
        String block = superAdminGuard(admin);
        if (block != null) return block;

        populateCommonModel(model, admin);
        model.addAttribute("adminUsers", userRepo.findByRole(User.Role.ADMIN));
        model.addAttribute("allHubs", hubRepo.findAll());
        return "admin/team";
    }

    @PostMapping("/team/create")
    public String createAdminUser(
            Principal principal,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String adminRole,
            @RequestParam(required = false) Long assignedHubId) {

        String block = superAdminGuard(getAdmin(principal));
        if (block != null) return block;

        if (userRepo.findByEmail(email) != null)
            return "redirect:/admin/team?error=EmailAlreadyExists";

        AdminRole role;
        try {
            role = AdminRole.valueOf(adminRole);
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/team?error=InvalidRole";
        }

        User newAdmin = new User();
        newAdmin.setFullName(fullName);
        newAdmin.setEmail(email);
        newAdmin.setPassword(
                org.springframework.security.crypto.bcrypt.BCrypt.hashpw(
                        password,
                        org.springframework.security.crypto.bcrypt.BCrypt.gensalt()));
        newAdmin.setRole(User.Role.ADMIN);
        newAdmin.setAdminRole(role);
        newAdmin.setVerificationStatus(VerificationStatus.APPROVED);
        newAdmin.setVerified(true);

        if (role == AdminRole.HUB_MANAGER && assignedHubId != null) {
            hubRepo.findById(assignedHubId).ifPresent(newAdmin::setAssignedHub);
        }

        userRepo.save(newAdmin);
        return "redirect:/admin/team?msg=AdminCreated";
    }

    @PostMapping("/team/update/{userId}")
    public String updateAdminRole(
            Principal principal,
            @PathVariable Long userId,
            @RequestParam String adminRole,
            @RequestParam(required = false) Long assignedHubId) {

        String block = superAdminGuard(getAdmin(principal));
        if (block != null) return block;

        AdminRole role;
        try {
            role = AdminRole.valueOf(adminRole);
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/team?error=InvalidRole";
        }

        User target = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        target.setAdminRole(role);

        if (role == AdminRole.HUB_MANAGER && assignedHubId != null) {
            hubRepo.findById(assignedHubId).ifPresent(target::setAssignedHub);
        } else {
            target.setAssignedHub(null);
        }

        userRepo.save(target);
        return "redirect:/admin/team?msg=RoleUpdated";
    }

    @PostMapping("/team/delete/{userId}")
    public String deleteAdminUser(@PathVariable Long userId, Principal principal) {
        User self = getAdmin(principal);
        String block = superAdminGuard(self);
        if (block != null) return block;

        if (self.getId().equals(userId))
            return "redirect:/admin/team?error=CannotDeleteSelf";

        userRepo.deleteById(userId);
        return "redirect:/admin/team?msg=AdminDeleted";
    }

    // ─────────────────────────────────────────
    //  VERIFICATIONS
    // ─────────────────────────────────────────

    @GetMapping("/verifications")
    public String verificationsPage(Principal principal, Model model) {
        User admin = getAdmin(principal);
        String block = superAdminGuard(admin);
        if (block != null) return block;

        populateCommonModel(model, admin);

        List<User> staffUsers = new ArrayList<>();
        staffUsers.addAll(userRepo.findByRole(User.Role.COOK));
        staffUsers.addAll(userRepo.findByRole(User.Role.DELIVERY_PARTNER));

        model.addAttribute("pendingUsers",  staffUsers.stream()
                .filter(u -> hasStatus(u, VerificationStatus.PENDING))
                .collect(Collectors.toList()));
        model.addAttribute("approvedUsers", staffUsers.stream()
                .filter(u -> hasStatus(u, VerificationStatus.APPROVED))
                .collect(Collectors.toList()));
        model.addAttribute("rejectedUsers", staffUsers.stream()
                .filter(u -> hasStatus(u, VerificationStatus.REJECTED)
                          || hasStatus(u, VerificationStatus.NOT_SUBMITTED))
                .collect(Collectors.toList()));

        Map<Long, VerificationDocument> docMap = new HashMap<>();
        verDocRepo.findAll().forEach(doc -> {
            if (doc != null && doc.getUser() != null && doc.getUser().getId() != null) {
                docMap.put(doc.getUser().getId(), doc);
            }
        });
        model.addAttribute("docMap", docMap);

        return "admin/verifications";
    }

    @PostMapping("/verifications/approve/{userId}")
    public String approveVerification(@PathVariable Long userId, Principal principal) {
        String block = superAdminGuard(getAdmin(principal));
        if (block != null) return block;

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setVerificationStatus(VerificationStatus.APPROVED);
        user.setVerified(true);
        userRepo.save(user);

        verDocRepo.findAll().stream()
                .filter(d -> d != null && d.getUser() != null
                        && userId.equals(d.getUser().getId()))
                .findFirst()
                .ifPresent(doc -> {
                    doc.setReviewedAt(LocalDateTime.now());
                    doc.setRejectionReason(null);
                    verDocRepo.save(doc);
                });

        return "redirect:/admin/verifications?msg=Approved";
    }

    @PostMapping("/verifications/reject/{userId}")
    public String rejectVerification(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "") String reason,
            Principal principal) {

        String block = superAdminGuard(getAdmin(principal));
        if (block != null) return block;

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setVerificationStatus(VerificationStatus.REJECTED);
        user.setVerified(false);
        userRepo.save(user);

        verDocRepo.findAll().stream()
                .filter(d -> d != null && d.getUser() != null
                        && userId.equals(d.getUser().getId()))
                .findFirst()
                .ifPresent(doc -> {
                    doc.setReviewedAt(LocalDateTime.now());
                    doc.setRejectionReason(reason.isBlank() ? "Rejected by admin." : reason);
                    verDocRepo.save(doc);
                });

        return "redirect:/admin/verifications?msg=Rejected";
    }
}