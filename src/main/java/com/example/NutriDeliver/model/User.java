package com.example.NutriDeliver.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password;

    private String fullName;
    private String hostelBlock;
    private String dietaryPreference;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean isVerified = false;

    @Column(name = "is_online", nullable = false)
    private boolean online = false;

    private Double walletBalance = 0.0;
    private Integer healthPoints = 0;

    @Column(columnDefinition = "TEXT")
    private String fullAddress;

    private String mobileNumber;
    private String cookPreference;
    private String vehicleType;

    // ── VERIFICATION STATUS ──────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private VerificationStatus verificationStatus = VerificationStatus.NOT_SUBMITTED;

    // ── ADMIN ROLE PRESET ────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_role")
    private AdminRole adminRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_hub_id")
    private Hub assignedHub;

    // ── NEW FIELDS TO FIX CRASHES ────────────────────────────────────
    
    @OneToOne(mappedBy = "student")
    private Subscription activeSubscription;

    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    // ── THYMELEAF UI FIELDS (Not saved to database) ──────────────────
    @Transient
    private Integer totalOrders = 0;

    // THIS IS THE FIELD YOU WERE MISSING!
    @Transient
    private Integer totalDeliveries = 0;

    // ── ENUM ─────────────────────────────────────────────────────────
    public enum Role {
        STUDENT, COOK, DELIVERY_PARTNER, ADMIN, HUB;
    }

    // ── GETTERS & SETTERS ─────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getHostelBlock() { return hostelBlock; }
    public void setHostelBlock(String hostelBlock) { this.hostelBlock = hostelBlock; }

    public String getDietaryPreference() { return dietaryPreference; }
    public void setDietaryPreference(String dietaryPreference) { this.dietaryPreference = dietaryPreference; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public Double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(Double walletBalance) { this.walletBalance = walletBalance; }

    public Integer getHealthPoints() { return healthPoints; }
    public void setHealthPoints(Integer healthPoints) { this.healthPoints = healthPoints; }

    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getCookPreference() { return cookPreference; }
    public void setCookPreference(String cookPreference) { this.cookPreference = cookPreference; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus == null ? VerificationStatus.NOT_SUBMITTED : verificationStatus;
    }
    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public AdminRole getAdminRole() { return adminRole; }
    public void setAdminRole(AdminRole adminRole) { this.adminRole = adminRole; }

    public Hub getAssignedHub() { return assignedHub; }
    public void setAssignedHub(Hub assignedHub) { this.assignedHub = assignedHub; }

    public Subscription getActiveSubscription() { return activeSubscription; }
    public void setActiveSubscription(Subscription activeSubscription) { this.activeSubscription = activeSubscription; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }

    // THESE ARE THE METHODS YOU WERE MISSING!
    public Integer getTotalDeliveries() { return totalDeliveries; }
    public void setTotalDeliveries(Integer totalDeliveries) { this.totalDeliveries = totalDeliveries; }
}