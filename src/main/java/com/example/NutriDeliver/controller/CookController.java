package com.example.NutriDeliver.controller;

import com.example.NutriDeliver.model.*;
import com.example.NutriDeliver.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cook")
public class CookController {

    @Autowired private OrderRepository      orderRepo;
    @Autowired private ShiftRepository      shiftRepo;
    @Autowired private UserRepository       userRepo;
    @Autowired private HubRepository        hubRepo;
    @Autowired private VerificationDocumentRepository verDocRepo;

    // ─────────────────────────── VERIFICATION GUARD ──────────────────────────

    /** Returns redirect string if cook is not verified, null if they're good to go. */
    private String verificationGuard(User user) {
        VerificationStatus status = user.getVerificationStatus();
        if (status == null || status != VerificationStatus.APPROVED) {
            return "redirect:/verify";
        }
        return null;
    }

    // ─────────────────────────── HELPERS ─────────────────────────────────────

    private void populateCommonModel(User cook, Model model) {
        model.addAttribute("isOnDuty", isCookOnDuty(cook));
        // Provide active hubs for the schedule/booking form
        model.addAttribute("activeHubs", hubRepo.findByActiveTrue());
    }

    private MealOrder.MealType getCurrentKitchenShift() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(10, 30))) return MealOrder.MealType.BREAKFAST;
        if (now.isBefore(LocalTime.of(15, 30))) return MealOrder.MealType.LUNCH;
        return MealOrder.MealType.DINNER;
    }

    private int getShiftStartHour(ShiftBooking.ShiftSlot slot) {
        return switch (slot) {
            case BREAKFAST -> 8;
            case LUNCH     -> 12;
            case DINNER    -> 19;
        };
    }

    private boolean isCookOnDuty(User cook) {
        return shiftRepo.findAll().stream()
                .filter(s -> s.getCook() != null && s.getCook().getId().equals(cook.getId()))
                .filter(s -> s.getShiftDate().equals(LocalDate.now()))
                .anyMatch(ShiftBooking::isCheckedIn);
    }

    private boolean orderMatchesCookPreference(MealOrder order, String pref) {
        if (order.getMealCategory() == null) return false;
        if ("Pure Veg".equals(pref)) return "Pure Veg".equals(order.getMealCategory());
        if ("Veg-Non-Veg Mix".equals(pref)) return true;
        return false;
    }

    // ─────────────────────────── DASHBOARD ───────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User cook = userRepo.findByEmail(principal.getName());
        String guard = verificationGuard(cook);
        if (guard != null) return guard;
        model.addAttribute("cook", cook);
        model.addAttribute("activeTab", "dashboard");
        populateCommonModel(cook, model);

        List<MealOrder> myPackedTiffins = orderRepo.findAll().stream()
                .filter(o -> o.getCook() != null && o.getCook().getId().equals(cook.getId()))
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().toLocalDate().equals(LocalDate.now()))
                .toList();

        model.addAttribute("dispatched", myPackedTiffins.size());
        model.addAttribute("earnings",   myPackedTiffins.size() * 15.0);

        long remaining = orderRepo.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .filter(o -> o.getMealType() == getCurrentKitchenShift())
                .filter(o -> orderMatchesCookPreference(o, cook.getCookPreference()))
                .count();
        model.addAttribute("remaining",     remaining);
        model.addAttribute("currentShift",  getCurrentKitchenShift().name());

        return "cook/dashboard";
    }

    // ─────────────────────────── SCHEDULE ────────────────────────────────────

    @GetMapping("/schedule")
    public String schedule(Principal principal, Model model) {
        User cook = userRepo.findByEmail(principal.getName());
        String guard = verificationGuard(cook);
        if (guard != null) return guard;
        model.addAttribute("cook", cook);
        model.addAttribute("activeTab", "schedule");
        populateCommonModel(cook, model);

        List<ShiftBooking> myBookings = shiftRepo.findAll().stream()
                .filter(b -> b.getCook() != null && b.getCook().getId().equals(cook.getId()))
                .filter(b -> b.getShiftDate().equals(LocalDate.now()))
                .collect(Collectors.toList());
        model.addAttribute("myBookings", myBookings);

        boolean hasBreakfast = myBookings.stream().anyMatch(b -> b.getSlot() == ShiftBooking.ShiftSlot.BREAKFAST);
        boolean hasLunch     = myBookings.stream().anyMatch(b -> b.getSlot() == ShiftBooking.ShiftSlot.LUNCH);
        boolean hasDinner    = myBookings.stream().anyMatch(b -> b.getSlot() == ShiftBooking.ShiftSlot.DINNER);

        model.addAttribute("hasBreakfast", hasBreakfast);
        model.addAttribute("hasLunch",     hasLunch);
        model.addAttribute("hasDinner",    hasDinner);

        LocalTime now = LocalTime.now();
        model.addAttribute("canBookBreakfast", now.isBefore(LocalTime.of(8,  0)) && !hasBreakfast);
        model.addAttribute("canBookLunch",     now.isBefore(LocalTime.of(12, 0)) && !hasLunch);
        model.addAttribute("canBookDinner",    now.isBefore(LocalTime.of(19, 0)) && !hasDinner);

        return "cook/dashboard";
    }

    // ─────────────────────────── BOOK SLOT ───────────────────────────────────

    /**
     * Cook selects a slot AND a hub when booking.
     * hubId is required — cook must pick a hub to cook at.
     */
    @PostMapping("/book-slot")
    public String bookSlot(
            @RequestParam String slotType,
            @RequestParam(required = false) Long hubId,
            Principal principal) {

        // Guard: if hubId missing (form submitted without selecting a hub)
        if (hubId == null)
            return "redirect:/cook/schedule?error=NoHubSelected";

        User cook = userRepo.findByEmail(principal.getName());
        ShiftBooking.ShiftSlot slot = ShiftBooking.ShiftSlot.valueOf(slotType);
        Hub hub = hubRepo.findById(hubId).orElse(null);

        if (hub == null || !hub.isActive())
            return "redirect:/cook/schedule?error=InvalidHub";

        boolean alreadyBooked = shiftRepo.findAll().stream()
                .anyMatch(b -> b.getCook() != null
                        && b.getCook().getId().equals(cook.getId())
                        && b.getShiftDate().equals(LocalDate.now())
                        && b.getSlot() == slot);
        if (alreadyBooked)
            return "redirect:/cook/schedule?error=SlotAlreadyBooked";

        LocalTime now = LocalTime.now();
        int startHour = getShiftStartHour(slot);
        if (now.isAfter(LocalTime.of(startHour, 0)))
            return "redirect:/cook/schedule?error=SlotTimePassed";

        ShiftBooking booking = new ShiftBooking();
        booking.setCook(cook);
        booking.setShiftDate(LocalDate.now());
        booking.setSlot(slot);
        booking.setHub(hub);
        booking.setEntryOtp(String.format("%04d", new Random().nextInt(10000)));
        shiftRepo.save(booking);

        return "redirect:/cook/schedule?msg=ShiftBooked";
    }

    // ─────────────────────────── CANCEL SLOT ─────────────────────────────────

    @PostMapping("/cancel-slot/{id}")
    public String cancelSlot(@PathVariable Long id, Principal principal) {
        User cook = userRepo.findByEmail(principal.getName());
        ShiftBooking booking = shiftRepo.findById(id).orElse(null);

        if (booking == null || !booking.getCook().getId().equals(cook.getId()))
            return "redirect:/cook/schedule?error=InvalidBooking";
        if (booking.isCheckedIn())
            return "redirect:/cook/schedule?error=CannotCancelCheckedInShift";

        int startHour = getShiftStartHour(booking.getSlot());
        LocalTime cancelDeadline = LocalTime.of(startHour, 0).minusHours(1);
        if (booking.getShiftDate().equals(LocalDate.now()) && LocalTime.now().isAfter(cancelDeadline))
            return "redirect:/cook/schedule?error=TooLateToCancel";

        shiftRepo.delete(booking);
        return "redirect:/cook/schedule?msg=ShiftCancelled";
    }

    // ─────────────────────────── HUB CHECK-IN ────────────────────────────────

    @PostMapping("/hub-checkin")
    public String checkIn(@RequestParam String otp, Principal principal) {
        User cook = userRepo.findByEmail(principal.getName());

        ShiftBooking targetBooking = shiftRepo.findAll().stream()
                .filter(b -> b.getCook() != null && b.getCook().getId().equals(cook.getId()))
                .filter(b -> b.getShiftDate().equals(LocalDate.now()) && b.getEntryOtp().equals(otp))
                .findFirst().orElse(null);

        if (targetBooking == null)
            return "redirect:/cook/schedule?error=InvalidEntryOTP";

        int startHour = getShiftStartHour(targetBooking.getSlot());
        if (LocalTime.now().isBefore(LocalTime.of(startHour, 0).minusMinutes(5)))
            return "redirect:/cook/schedule?error=TooEarlyToCheckIn";

        targetBooking.setCheckedIn(true);
        shiftRepo.save(targetBooking);
        return "redirect:/cook/prep-list?msg=KitchenUnlocked";
    }

    // ─────────────────────────── PREP LIST ───────────────────────────────────

    @GetMapping("/prep-list")
    public String prepList(Principal principal, Model model) {
        User cook = userRepo.findByEmail(principal.getName());
        String guard = verificationGuard(cook);
        if (guard != null) return guard;
        model.addAttribute("cook", cook);
        model.addAttribute("activeTab", "prep");
        populateCommonModel(cook, model);

        model.addAttribute("currentShift", getCurrentKitchenShift().name());

        if (isCookOnDuty(cook)) {
            List<MealOrder> pendingOrders = orderRepo.findAll().stream()
                    .filter(o -> o.getStatus() == OrderStatus.PENDING)
                    .filter(o -> o.getMealType() == getCurrentKitchenShift())
                    .filter(o -> orderMatchesCookPreference(o, cook.getCookPreference()))
                    .collect(Collectors.toList());
            model.addAttribute("pendingOrders", pendingOrders);
        }
        return "cook/dashboard";
    }

    @PostMapping("/order/pack/{id}")
    public String packOrder(@PathVariable Long id, Principal principal) {
        User cook = userRepo.findByEmail(principal.getName());
        if (!isCookOnDuty(cook)) return "redirect:/cook/prep-list?error=NotOnDuty";

        MealOrder order = orderRepo.findById(id).orElseThrow();
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.PACKED);
            order.setCook(cook);
            orderRepo.save(order);
        }
        return "redirect:/cook/prep-list";
    }

    // ─────────────────────────── PROFILE ─────────────────────────────────────

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User cook = userRepo.findByEmail(principal.getName());
        String guard = verificationGuard(cook);
        if (guard != null) return guard;
        model.addAttribute("cook", cook);
        model.addAttribute("activeTab", "profile");
        populateCommonModel(cook, model);
        return "cook/dashboard";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute User updatedData, Principal principal) {
        User cook = userRepo.findByEmail(principal.getName());
        cook.setFullName(updatedData.getFullName());
        cook.setMobileNumber(updatedData.getMobileNumber());
        cook.setFullAddress(updatedData.getFullAddress());
        cook.setCookPreference(updatedData.getCookPreference());
        userRepo.save(cook);
        return "redirect:/cook/profile?msg=ProfileUpdated";
    }
}