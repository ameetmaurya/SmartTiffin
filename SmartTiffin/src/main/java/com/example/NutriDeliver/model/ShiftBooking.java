package com.example.NutriDeliver.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class ShiftBooking {

    public enum ShiftSlot { BREAKFAST, LUNCH, DINNER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cook_id")
    private User cook;

    private LocalDate shiftDate;

    @Enumerated(EnumType.STRING)
    private ShiftSlot slot;

    private String entryOtp;

    @Column(name = "checked_in", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean checkedIn = false;

    // Delivery partner assigned to this shift (null for cook bookings)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    // NEW: which hub this booking is for
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    private Hub hub;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCook() { return cook; }
    public void setCook(User cook) { this.cook = cook; }

    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }

    public LocalDate getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }

    public ShiftSlot getSlot() { return slot; }
    public void setSlot(ShiftSlot slot) { this.slot = slot; }

    public String getEntryOtp() { return entryOtp; }
    public void setEntryOtp(String entryOtp) { this.entryOtp = entryOtp; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public Hub getHub() { return hub; }
    public void setHub(Hub hub) { this.hub = hub; }
}