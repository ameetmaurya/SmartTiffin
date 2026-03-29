package com.example.NutriDeliver.service;

import com.example.NutriDeliver.model.*;
import com.example.NutriDeliver.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    // Strict Cancellation Logic
    public void cancelOrder(Long orderId, User user) {
        MealOrder order = orderRepository.findById(orderId).orElseThrow();

        // Rule: 11:00 AM Deadline
        LocalTime deadline = LocalTime.of(11, 0);
        if (LocalDateTime.now().toLocalTime().isAfter(deadline) &&
                order.getOrderDate().toLocalDate().equals(LocalDateTime.now().toLocalDate())) {
            throw new RuntimeException("Too late! Cancellation closes at 11:00 AM.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        // Gamification Logic: Reset streak?
        orderRepository.save(order);
    }

    // Auto-Routing: Cluster Logic
    public List<MealOrder> getOptimizedBatchForDriver(String hostelBlock) {
        // Returns orders grouped by specific hostel block
        return orderRepository.findByHostelClusterAndStatus(hostelBlock, OrderStatus.PACKED);
    }
}