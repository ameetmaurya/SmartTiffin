package com.example.NutriDeliver.repository;

import com.example.NutriDeliver.model.MealOrder;
import com.example.NutriDeliver.model.OrderStatus;
import com.example.NutriDeliver.model.User; // Import User model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<MealOrder, Long> {

    List<MealOrder> findByStudentId(Long studentId);

    // For Delivery Dashboard: Find all ready orders waiting for a driver
    List<MealOrder> findByStatus(OrderStatus status);

    // For Delivery Dashboard: Find orders assigned to a specific driver
    List<MealOrder> findByDeliveryPartnerAndStatus(User deliveryPartner, OrderStatus status);
    List<MealOrder> findByHostelClusterAndStatus(String hostelCluster, OrderStatus status);

    List<MealOrder> findByOrderDateBetweenAndMealTypeAndStatus(LocalDateTime start, LocalDateTime end, MealOrder.MealType mealType, OrderStatus orderStatus);
}