package com.example.NutriDeliver.model;

import com.example.NutriDeliver.model.OrderStatus;
import com.example.NutriDeliver.model.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class MealOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    private LocalDateTime orderDate;
    private String items;

    // NEW: Enum to define the slot
    public enum MealType { BREAKFAST, LUNCH, DINNER }
    @Enumerated(EnumType.STRING)
    private MealType mealType;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String mealCategory; // NEW: "Pure Veg" or "Non-Veg"

    private String deliveryOtp;
    private String hostelCluster;

    @ManyToOne
    @JoinColumn(name = "delivery_partner_id")
    private User deliveryPartner;
    @ManyToOne
    @JoinColumn(name = "cook_id")
    private User cook; // NEW: Tracks which cook packed this meal

    public String getMealCategory() {
        return mealCategory;
    }

    public void setMealCategory(String mealCategory) {
        this.mealCategory = mealCategory;
    }

    public User getCook() {
        return cook;
    }

    public void setCook(User cook) {
        this.cook = cook;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public MealType getMealType() {
        return mealType;
    }

    public void setMealType(MealType mealType) {
        this.mealType = mealType;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getDeliveryOtp() {
        return deliveryOtp;
    }

    public void setDeliveryOtp(String deliveryOtp) {
        this.deliveryOtp = deliveryOtp;
    }

    public String getHostelCluster() {
        return hostelCluster;
    }

    public void setHostelCluster(String hostelCluster) {
        this.hostelCluster = hostelCluster;
    }

    public User getDeliveryPartner() {
        return deliveryPartner;
    }

    public void setDeliveryPartner(User deliveryPartner) {
        this.deliveryPartner = deliveryPartner;
    }
}