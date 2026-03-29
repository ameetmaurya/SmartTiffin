package com.example.NutriDeliver.repository;

import com.example.NutriDeliver.model.ShiftBooking;
import com.example.NutriDeliver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<ShiftBooking, Long> {

    // ── Used by DeliveryController ──

    // Find the active checked-in shift for a driver today
    Optional<ShiftBooking> findByDriverAndShiftDateAndCheckedInTrue(
            User driver, LocalDate shiftDate);

    // Count drivers already booked for a slot (capacity check)
    long countByShiftDateAndSlot(
            LocalDate shiftDate, ShiftBooking.ShiftSlot slot);

    // ── Used by AdminController / general queries ──

    List<ShiftBooking> findByShiftDate(LocalDate shiftDate);
}