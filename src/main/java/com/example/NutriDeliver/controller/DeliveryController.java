package com.example.NutriDeliver.controller;

import com.example.NutriDeliver.model.MealOrder;
import com.example.NutriDeliver.model.OrderStatus;
import com.example.NutriDeliver.model.ShiftBooking;
import com.example.NutriDeliver.model.User;
import com.example.NutriDeliver.model.VerificationStatus;
import com.example.NutriDeliver.repository.OrderRepository;
import com.example.NutriDeliver.repository.ShiftRepository;
import com.example.NutriDeliver.repository.UserRepository;
import com.example.NutriDeliver.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Controller
@RequestMapping("/delivery")
public class DeliveryController {

    @Autowired private UserRepository userRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private ShiftRepository shiftRepo;
    @Autowired private DeliveryService deliveryService;

    // ─────────────────────────── VERIFICATION GUARD ──────────────────

    private String verificationGuard(User driver) {
        VerificationStatus status = driver.getVerificationStatus();
        if (status == null || status != VerificationStatus.APPROVED)
            return "redirect:/verify";
        return null;
    }

    // ─────────────────────────── HELPER ──────────────────────────────

    private ShiftBooking getActiveShift(User driver) {
        return shiftRepo.findByDriverAndShiftDateAndCheckedInTrue(driver, LocalDate.now())
                .orElse(null);
    }

    // ─────────────────────────── DASHBOARD ───────────────────────────

    @GetMapping("/schedule")
    public String dashboard(Principal principal, Model model) {
        User driver = userRepo.findByEmail(principal.getName());

        // ── VERIFICATION GUARD ──
        String guard = verificationGuard(driver);
        if (guard != null) return guard;

        model.addAttribute("driver", driver);

        LocalTime now = LocalTime.now();
        model.addAttribute("canBookBreakfast", now.isBefore(LocalTime.of(12, 0)));
        model.addAttribute("canBookLunch",     now.isBefore(LocalTime.of(14, 0)));
        model.addAttribute("canBookDinner",    now.isBefore(LocalTime.of(21, 0)));

        ShiftBooking activeShift = getActiveShift(driver);

        if (activeShift != null) {
            model.addAttribute("isOnDuty", true);
            model.addAttribute("activeSlot", activeShift.getSlot().name());

            MealOrder.MealType driverMealType =
                    MealOrder.MealType.valueOf(activeShift.getSlot().name());

            List<MealOrder> availableOrders = orderRepo.findByStatus(OrderStatus.PACKED)
                    .stream()
                    .filter(o -> o.getMealType() == driverMealType)
                    .toList();

            List<MealOrder> myDeliveries =
                    orderRepo.findByDeliveryPartnerAndStatus(driver, OrderStatus.OUT_FOR_DELIVERY);

            model.addAttribute("availableOrders", availableOrders);
            model.addAttribute("myDeliveries",    myDeliveries);

        } else {
            model.addAttribute("isOnDuty", false);

            List<ShiftBooking> myBookings = shiftRepo.findAll().stream()
                    .filter(b -> b.getDriver() != null
                              && b.getDriver().getId().equals(driver.getId())
                              && !b.getShiftDate().isBefore(LocalDate.now()))
                    .toList();
            model.addAttribute("myBookings", myBookings);
        }

        return "delivery/schedule";
    }

    // ─────────────────────────── BOOK SLOT ───────────────────────────

    @PostMapping("/book-slot")
    public String bookSlot(@RequestParam String slotType, Principal principal) {
        User driver = userRepo.findByEmail(principal.getName());

        String guard = verificationGuard(driver);
        if (guard != null) return guard;

        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        if (!driver.isOnline())
            return "redirect:/delivery/schedule?error=MustBeOnline";

        ShiftBooking.ShiftSlot slot = ShiftBooking.ShiftSlot.valueOf(slotType);

        if (slot == ShiftBooking.ShiftSlot.BREAKFAST && now.isAfter(LocalTime.of(12, 0)))
            return "redirect:/delivery/schedule?error=BreakfastShiftEnded";
        if (slot == ShiftBooking.ShiftSlot.LUNCH     && now.isAfter(LocalTime.of(14, 0)))
            return "redirect:/delivery/schedule?error=LunchShiftEnded";
        if (slot == ShiftBooking.ShiftSlot.DINNER    && now.isAfter(LocalTime.of(21, 0)))
            return "redirect:/delivery/schedule?error=DinnerShiftEnded";

        boolean alreadyBooked = shiftRepo.findAll().stream()
                .anyMatch(b -> b.getDriver() != null
                            && b.getDriver().getId().equals(driver.getId())
                            && b.getShiftDate().equals(today)
                            && b.getSlot() == slot);
        if (alreadyBooked) return "redirect:/delivery/schedule?error=AlreadyBooked";

        long currentCount = shiftRepo.countByShiftDateAndSlot(today, slot);
        if (currentCount >= 5) return "redirect:/delivery/schedule?error=SlotFull";

        ShiftBooking booking = new ShiftBooking();
        booking.setDriver(driver);
        booking.setShiftDate(today);
        booking.setSlot(slot);
        booking.setEntryOtp(String.format("%04d", new Random().nextInt(9000) + 1000));
        shiftRepo.save(booking);

        return "redirect:/delivery/schedule?msg=SlotBooked";
    }

    // ─────────────────────────── HUB CHECK-IN ────────────────────────

    @PostMapping("/hub-checkin")
    public String checkIn(@RequestParam String otp, Principal principal) {
        User driver = userRepo.findByEmail(principal.getName());

        String guard = verificationGuard(driver);
        if (guard != null) return guard;

        LocalDate today = LocalDate.now();

        ShiftBooking targetBooking = shiftRepo.findAll().stream()
                .filter(b -> b.getDriver() != null
                          && b.getDriver().getId().equals(driver.getId()))
                .filter(b -> b.getShiftDate().equals(today)
                          && b.getEntryOtp().equals(otp))
                .findFirst().orElse(null);

        if (targetBooking == null)
            return "redirect:/delivery/schedule?error=InvalidEntryOTP";

        targetBooking.setCheckedIn(true);
        shiftRepo.save(targetBooking);

        return "redirect:/delivery/schedule?msg=OnDuty";
    }

    // ─────────────────────────── ACCEPT JOB ──────────────────────────

    @PostMapping("/accept/{id}")
    public String acceptOrder(@PathVariable Long id, Principal principal) {
        User driver = userRepo.findByEmail(principal.getName());

        String guard = verificationGuard(driver);
        if (guard != null) return guard;

        ShiftBooking activeShift = getActiveShift(driver);
        if (activeShift == null)
            return "redirect:/delivery/schedule?error=NotOnDuty";

        MealOrder order = orderRepo.findById(id).orElseThrow();

        if (order.getStatus() != OrderStatus.PACKED)
            return "redirect:/delivery/schedule?error=OrderNotAvailable";

        order.setDeliveryPartner(driver);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepo.save(order);

        return "redirect:/delivery/schedule?msg=JobAccepted";
    }

    // ─────────────────────────── VERIFY OTP (DELIVER) ────────────────

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam Long orderId,
                             @RequestParam String otp,
                             Principal principal) {
        User driver = userRepo.findByEmail(principal.getName());

        String guard = verificationGuard(driver);
        if (guard != null) return guard;

        MealOrder order = orderRepo.findById(orderId).orElseThrow();

        if (order.getDeliveryPartner() == null
                || !order.getDeliveryPartner().getId().equals(driver.getId()))
            return "redirect:/delivery/schedule?error=NotYourOrder";

        boolean isSuccess = deliveryService.verifyDelivery(orderId, otp, driver);
        if (isSuccess) return "redirect:/delivery/schedule?msg=PaymentCredited";
        return "redirect:/delivery/schedule?error=InvalidOTP";
    }

    // ─────────────────────────── END SHIFT ───────────────────────────

    @PostMapping("/end-shift")
    public String endShift(Principal principal) {
        User driver = userRepo.findByEmail(principal.getName());

        String guard = verificationGuard(driver);
        if (guard != null) return guard;

        ShiftBooking activeShift = getActiveShift(driver);
        if (activeShift == null)
            return "redirect:/delivery/schedule?error=NotOnDuty";

        boolean hasActiveDelivery = orderRepo
                .findByDeliveryPartnerAndStatus(driver, OrderStatus.OUT_FOR_DELIVERY)
                .size() > 0;
        if (hasActiveDelivery)
            return "redirect:/delivery/schedule?error=CompleteDeliveryFirst";

        shiftRepo.delete(activeShift);
        return "redirect:/delivery/schedule?msg=ShiftEnded";
    }

    // ─────────────────────────── CANCEL SLOT ─────────────────────────

    @PostMapping("/cancel-slot/{id}")
    public String cancelSlot(@PathVariable Long id, Principal principal) {
        User driver = userRepo.findByEmail(principal.getName());

        String guard = verificationGuard(driver);
        if (guard != null) return guard;

        ShiftBooking booking = shiftRepo.findById(id).orElse(null);

        if (booking == null || booking.getDriver() == null
                || !booking.getDriver().getId().equals(driver.getId()))
            return "redirect:/delivery/schedule?error=InvalidBooking";

        if (booking.isCheckedIn())
            return "redirect:/delivery/schedule?error=CannotCancelCheckedIn";

        shiftRepo.delete(booking);
        return "redirect:/delivery/schedule?msg=SlotCancelled";
    }

    // ─────────────────────────── GO ONLINE / OFFLINE ─────────────────

    @PostMapping("/go-online")
    public String goOnline(Principal principal) {
        User driver = userRepo.findByEmail(principal.getName());
        String guard = verificationGuard(driver);
        if (guard != null) return guard;
        driver.setOnline(true);
        userRepo.save(driver);
        return "redirect:/delivery/schedule?msg=NowOnline";
    }

    @PostMapping("/go-offline")
    public String goOffline(Principal principal) {
        User driver = userRepo.findByEmail(principal.getName());
        driver.setOnline(false);
        userRepo.save(driver);
        return "redirect:/delivery/schedule?msg=NowOffline";
    }
}