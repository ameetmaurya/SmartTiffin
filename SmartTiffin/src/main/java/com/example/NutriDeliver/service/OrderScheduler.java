package com.example.NutriDeliver.service;



import com.example.NutriDeliver.model.MealOrder;
import com.example.NutriDeliver.model.OrderStatus;
import com.example.NutriDeliver.model.Subscription;
import com.example.NutriDeliver.repository.OrderRepository;
import com.example.NutriDeliver.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class OrderScheduler {

    @Autowired private SubscriptionRepository subRepo;
    @Autowired private OrderRepository orderRepo;

    // Runs every day at Midnight (00:00:00)
    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyTiffins() {
        System.out.println("--- SCHEDULER STARTED: Generating Daily Orders ---");

        List<Subscription> activeSubs = subRepo.findByIsActiveTrue();
        LocalDate today = LocalDate.now();

        for (Subscription sub : activeSubs) {

            // 1. Check if Plan Expired
            if (today.isAfter(sub.getEndDate())) {
                sub.setActive(false);
                subRepo.save(sub);
                continue;
            }

            // 2. Check "Skip Meal" Logic
            if (sub.getNextSkipDate() != null && sub.getNextSkipDate().equals(today)) {
                System.out.println("Skipping order for: " + sub.getStudent().getFullName());
                // Reset skip date after skipping
                sub.setNextSkipDate(null);
                subRepo.save(sub);
                continue;
            }

            // 3. Create the Order Automatically
            MealOrder autoOrder = new MealOrder();
            autoOrder.setStudent(sub.getStudent());
            autoOrder.setOrderDate(LocalDateTime.now()); // Breakfast/Lunch time
            autoOrder.setItems("Daily Subscription Meal (Standard Thali)");
            autoOrder.setStatus(OrderStatus.PENDING);

            // Generate OTP
            int otp = new Random().nextInt(90) + 10;
            autoOrder.setDeliveryOtp(String.valueOf(otp));

            // Set Cluster
            String block = sub.getStudent().getHostelBlock();
            autoOrder.setHostelCluster((block != null) ? block : "General");

            orderRepo.save(autoOrder);
            System.out.println("Generated Order for: " + sub.getStudent().getFullName());
        }
    }
}