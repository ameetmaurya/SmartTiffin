package com.example.NutriDeliver.controller;

import com.example.NutriDeliver.model.*;
import com.example.NutriDeliver.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class MainControllers {

    @Autowired private UserRepository         userRepo;
    @Autowired private OrderRepository        orderRepo;
    @Autowired private PasswordEncoder        passwordEncoder;
    @Autowired private SubscriptionRepository subRepo;
    @Autowired private ReviewRepository       reviewRepo;
    @Autowired private WeeklyMenuRepository   weeklyMenuRepo;

    // ─────────────────────────── PUBLIC PAGES ───────────────────────────

    @GetMapping("/")
    public String landingPage() { return "index"; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // ─────────────────────────── REGISTRATION ───────────────────────────

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        if (userRepo.findByEmail(user.getEmail()) != null) {
            model.addAttribute("user", user);
            model.addAttribute("error", "EmailAlreadyExists");
            return "register";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getWalletBalance() == null) user.setWalletBalance(0.0);
        if (user.getHealthPoints()  == null) user.setHealthPoints(0);
        userRepo.save(user);
        return "redirect:/login?registered";
    }

    // ─────────────────────────── DIET FILTER HELPER ─────────────────────

    private List<WeeklyMenu> filterByDiet(List<WeeklyMenu> menus, String diet) {
        return menus.stream().filter(m -> {
            if ("Pure Veg".equals(diet)) {
                return "Pure Veg".equals(m.getMealCategory());
            } else {
                if ("Non-Veg".equals(m.getMealCategory())) return true;
                boolean hasNonVegAlternative = menus.stream()
                        .anyMatch(other ->
                                other.getDayOfWeek() == m.getDayOfWeek()
                             && other.getMealType()  == m.getMealType()
                             && "Non-Veg".equals(other.getMealCategory()));
                return !hasNonVegAlternative;
            }
        }).collect(Collectors.toList());
    }

    // ─────────────────────── HELPER: get subscription safely ────────────

    /**
     * Always use this instead of subRepo.findByStudentId() directly.
     * Returns null if no subscription exists — never throws.
     */
    private Subscription getSub(Long userId) {
        return subRepo.findByStudentId(userId).orElse(null);
    }

    // ─────────────────────────── STUDENT HOME ───────────────────────────

    @GetMapping("/customer/home")
    public String studentHome(Principal principal, Model model) {
        User user = userRepo.findByEmail(principal.getName());
        if (user == null) return "redirect:/login";

        if (user.getWalletBalance()     == null) user.setWalletBalance(0.0);
        if (user.getHealthPoints()      == null) user.setHealthPoints(0);
        if (user.getDietaryPreference() == null) user.setDietaryPreference("Pure Veg");
        userRepo.save(user);

        model.addAttribute("user", user);

        Subscription sub = getSub(user.getId());
        model.addAttribute("subscription", sub);

        boolean subPaused = (sub != null && sub.isActive() && sub.isPaused());
        model.addAttribute("subPaused", subPaused);

        LocalTime now = LocalTime.now();
        model.addAttribute("canOrderBreakfast", now.isBefore(LocalTime.of(10, 0)));
        model.addAttribute("canOrderLunch",     now.isBefore(LocalTime.of(14, 0)));
        model.addAttribute("canOrderDinner",    now.isBefore(LocalTime.of(21, 0)));

        DayOfWeek todayDay = LocalDate.now().getDayOfWeek();
        model.addAttribute("todayName", todayDay.name());

        List<WeeklyMenu> todaysMenusRaw = weeklyMenuRepo.findByDayOfWeek(todayDay);
        model.addAttribute("todaysMenus", filterByDiet(todaysMenusRaw, user.getDietaryPreference()));

        List<WeeklyMenu> fullMenuRaw  = weeklyMenuRepo.findAll();
        List<WeeklyMenu> filteredMenu = filterByDiet(fullMenuRaw, user.getDietaryPreference());

        Map<DayOfWeek, Map<String, WeeklyMenu>> weeklyPlan = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) weeklyPlan.put(day, new HashMap<>());
        for (WeeklyMenu m : filteredMenu)
            weeklyPlan.get(m.getDayOfWeek()).put(m.getMealType().name(), m);
        model.addAttribute("weeklyPlan", weeklyPlan);

        model.addAttribute("myOrders", orderRepo.findByStudentId(user.getId()));

        return "student/home";
    }

    // ─────────────────────────── PLACE ORDER ────────────────────────────

    @PostMapping("/customer/order/place")
    public String placeOrder(@RequestParam("mealType") String mealTypeStr, Principal principal) {
        User user = userRepo.findByEmail(principal.getName());
        Subscription sub = getSub(user.getId());

        if (sub == null || !sub.isActive() || sub.getRemainingMeals() <= 0)
            return "redirect:/customer/home?error=NoActivePlanOrMeals";

        if (sub.isPaused())
            return "redirect:/customer/home?error=PlanPaused";

        MealOrder.MealType mealType = MealOrder.MealType.valueOf(mealTypeStr);
        LocalTime now = LocalTime.now();

        if (mealType == MealOrder.MealType.BREAKFAST && now.isAfter(LocalTime.of(10, 0)))
            return "redirect:/customer/home?error=BreakfastOver";
        if (mealType == MealOrder.MealType.LUNCH     && now.isAfter(LocalTime.of(14, 0)))
            return "redirect:/customer/home?error=LunchOver";
        if (mealType == MealOrder.MealType.DINNER    && now.isAfter(LocalTime.of(21, 0)))
            return "redirect:/customer/home?error=DinnerOver";

        boolean alreadyOrdered = orderRepo.findByStudentId(user.getId()).stream()
                .anyMatch(o -> o.getMealType() == mealType
                        && o.getOrderDate() != null
                        && o.getOrderDate().toLocalDate().equals(LocalDate.now())
                        && o.getStatus() != OrderStatus.CANCELLED);
        if (alreadyOrdered)
            return "redirect:/customer/home?error=AlreadyOrdered";

        sub.setRemainingMeals(sub.getRemainingMeals() - 1);
        subRepo.save(sub);

        String diet = user.getDietaryPreference();
        WeeklyMenu todayMenu = weeklyMenuRepo
                .findByDayOfWeekAndMealTypeAndMealCategory(LocalDate.now().getDayOfWeek(), mealType, diet)
                .orElse(null);
        if (todayMenu == null && "Non-Veg".equals(diet)) {
            todayMenu = weeklyMenuRepo
                    .findByDayOfWeekAndMealTypeAndMealCategory(LocalDate.now().getDayOfWeek(), mealType, "Pure Veg")
                    .orElse(null);
        }

        MealOrder order = new MealOrder();
        order.setStudent(user);
        order.setOrderDate(LocalDateTime.now());
        order.setMealType(mealType);
        order.setItems(todayMenu != null ? todayMenu.getItems() : mealTypeStr + " Standard Meal");
        order.setMealCategory(todayMenu != null && todayMenu.getMealCategory() != null
                ? todayMenu.getMealCategory() : "Pure Veg");
        order.setStatus(OrderStatus.PENDING);
        order.setHostelCluster(user.getHostelBlock() != null ? user.getHostelBlock() : "General");
        order.setDeliveryOtp(String.format("%04d", new Random().nextInt(9000) + 1000));
        orderRepo.save(order);

        return "redirect:/customer/home?msg=OrderPlaced";
    }

    // ─────────────────────────── CANCEL ORDER ───────────────────────────

    @PostMapping("/customer/cancel/{id}")
    public String cancelOrder(@PathVariable Long id, Principal principal) {
        User user = userRepo.findByEmail(principal.getName());
        MealOrder order = orderRepo.findById(id).orElseThrow();

        if (!order.getStudent().getId().equals(user.getId()))
            return "redirect:/customer/home?error=NotYourOrder";
        if (order.getStatus() != OrderStatus.PENDING)
            return "redirect:/customer/home?error=TooLateToSkip";

        order.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(order);

        Subscription sub = getSub(user.getId());
        if (sub != null) {
            sub.setRemainingMeals(sub.getRemainingMeals() + 1);
            subRepo.save(sub);
        }
        return "redirect:/customer/home?msg=SkippedAndRefunded";
    }

    // ─────────────────────────── DONATE ORDER ───────────────────────────

    @PostMapping("/customer/donate/{id}")
    public String donateOrder(@PathVariable Long id, Principal principal) {
        User user = userRepo.findByEmail(principal.getName());
        MealOrder order = orderRepo.findById(id).orElseThrow();

        if (!order.getStudent().getId().equals(user.getId()))
            return "redirect:/customer/home?error=NotYourOrder";

        boolean canDonate = order.getStatus() == OrderStatus.PENDING
                         || order.getStatus() == OrderStatus.PACKED;
        if (!canDonate) return "redirect:/customer/home?error=CannotDonate";

        order.setStatus(OrderStatus.DONATED);
        orderRepo.save(order);
        user.setHealthPoints(user.getHealthPoints() + 50);
        userRepo.save(user);
        return "redirect:/customer/home?msg=MealDonated";
    }

    // ──────────────────── SUBSCRIPTION MANAGEMENT ───────────────────────

    /**
     * Activate (first time) or Reactivate (after cancel/expiry).
     *
     * FIX: We REUSE the existing Subscription row instead of creating a new
     * one. This avoids violating the @OneToOne unique constraint on student_id
     * which was the root cause of the 404 / ConstraintViolationException.
     */
    @PostMapping("/customer/subscribe")
    public String buySubscription(Principal principal) {
        User user = userRepo.findByEmail(principal.getName());

        if (user.getWalletBalance() < 2500.0)
            return "redirect:/customer/home?error=LowBalance";

        Subscription sub = getSub(user.getId());

        // Block only if a genuinely running plan exists
        if (sub != null && sub.isActive() && !sub.isPaused())
            return "redirect:/customer/home?error=AlreadySubscribed";

        // Deduct wallet
        user.setWalletBalance(user.getWalletBalance() - 2500.0);
        userRepo.save(user);

        // ✅ Reuse existing row OR create first-time row
        if (sub == null) {
            sub = new Subscription();
            sub.setStudent(user);
        }
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusDays(30));
        sub.setRemainingMeals(90);
        sub.setActive(true);
        sub.setPaused(false);
        sub.setNextSkipDate(null);
        subRepo.save(sub);

        return "redirect:/customer/home?msg=PlanActivated";
    }

    /** Pause plan — stops deliveries without cancelling. */
    @PostMapping("/customer/subscription/pause")
    public String pauseSubscription(Principal principal) {
        User user = userRepo.findByEmail(principal.getName());
        Subscription sub = getSub(user.getId());

        if (sub == null || !sub.isActive())
            return "redirect:/customer/home?error=NoPlan";
        if (sub.isPaused())
            return "redirect:/customer/home?error=AlreadyPaused";

        sub.setPaused(true);
        subRepo.save(sub);
        return "redirect:/customer/home?msg=PlanPaused";
    }

    /** Resume plan — deliveries continue from the next slot. */
    @PostMapping("/customer/subscription/resume")
    public String resumeSubscription(Principal principal) {
        User user = userRepo.findByEmail(principal.getName());
        Subscription sub = getSub(user.getId());

        if (sub == null || !sub.isActive())
            return "redirect:/customer/home?error=NoPlan";
        if (!sub.isPaused())
            return "redirect:/customer/home?error=NotPaused";

        sub.setPaused(false);
        subRepo.save(sub);
        return "redirect:/customer/home?msg=PlanResumed";
    }

    /** Cancel plan — marks inactive; remaining meals are forfeited. */
    @PostMapping("/customer/subscription/cancel")
    public String cancelSubscription(Principal principal) {
        User user = userRepo.findByEmail(principal.getName());
        Subscription sub = getSub(user.getId());

        if (sub == null || !sub.isActive())
            return "redirect:/customer/home?error=NoPlan";

        sub.setActive(false);
        sub.setPaused(false);
        subRepo.save(sub);
        return "redirect:/customer/home?msg=PlanCancelled";
    }

    // ─────────────────────────── WALLET ─────────────────────────────────

    @GetMapping("/customer/wallet")
    public String showWalletPage(Principal principal, Model model) {
        model.addAttribute("user", userRepo.findByEmail(principal.getName()));
        return "student/wallet";
    }

    @PostMapping("/customer/wallet/add")
    public String addMoney(@RequestParam Double amount, Principal principal) {
        if (amount <= 0 || amount > 50000)
            return "redirect:/customer/home?error=InvalidAmount";
        User user = userRepo.findByEmail(principal.getName());
        user.setWalletBalance(user.getWalletBalance() + amount);
        userRepo.save(user);
        return "redirect:/customer/home?msg=PaymentSuccess";
    }

    // ─────────────────────────── PROFILE ────────────────────────────────

    @PostMapping("/customer/profile/update")
    public String updateProfile(@ModelAttribute User updatedData, Principal principal) {
        User user = userRepo.findByEmail(principal.getName());
        user.setFullName(updatedData.getFullName());
        user.setMobileNumber(updatedData.getMobileNumber());
        user.setFullAddress(updatedData.getFullAddress());
        user.setDietaryPreference(updatedData.getDietaryPreference());
        userRepo.save(user);
        return "redirect:/customer/home?msg=ProfileUpdated";
    }

    // ─────────────────────────── REVIEW ─────────────────────────────────

    @PostMapping("/customer/review")
    public String submitReview(
            @RequestParam Long orderId,
            @RequestParam int rating,
            @RequestParam String comments,
            Principal principal) {

        if (rating < 1 || rating > 5)
            return "redirect:/customer/home?error=InvalidRating";

        MealOrder order = orderRepo.findById(orderId).orElseThrow();
        User user = userRepo.findByEmail(principal.getName());

        if (!order.getStudent().getId().equals(user.getId()))
            return "redirect:/customer/home?error=NotYourOrder";

        Review review = new Review();
        review.setOrder(order);
        review.setRating(rating);
        review.setComments(comments);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepo.save(review);

        user.setHealthPoints(user.getHealthPoints() + 10);
        userRepo.save(user);
        return "redirect:/customer/home?msg=ReviewSubmitted";
    }

    // ──────────────────── REAL-TIME ORDER TRACKING ───────────────────────

    @GetMapping("/customer/order/{id}/track")
    @ResponseBody
    public Map<String, Object> trackOrder(@PathVariable Long id, Principal principal) {
        User user = userRepo.findByEmail(principal.getName());
        MealOrder order = orderRepo.findById(id).orElseThrow();

        if (!order.getStudent().getId().equals(user.getId()))
            return Collections.singletonMap("error", "NotYourOrder");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId",  order.getId());
        response.put("status",   order.getStatus().name());
        response.put("mealType", order.getMealType().name());
        response.put("items",    order.getItems());
        response.put("otp",      order.getDeliveryOtp());

        if (order.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
            double baseLat = 18.9270;
            double baseLon = 72.8390;
            Random rng = new Random();
            response.put("driverLat", baseLat - 0.003 + rng.nextDouble() * 0.001);
            response.put("driverLon", baseLon - 0.003 + rng.nextDouble() * 0.001);
            response.put("destLat",   baseLat);
            response.put("destLon",   baseLon);
        }
        return response;
    }
}