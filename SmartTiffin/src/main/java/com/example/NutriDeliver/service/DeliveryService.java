package com.example.NutriDeliver.service;

import com.example.NutriDeliver.model.MealOrder;
import com.example.NutriDeliver.model.OrderStatus;
import com.example.NutriDeliver.model.User;
import com.example.NutriDeliver.repository.OrderRepository;
import com.example.NutriDeliver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    @Autowired private OrderRepository orderRepo;
    @Autowired private UserRepository userRepo;

    @Transactional
    public boolean verifyDelivery(Long orderId, String inputOtp, User driver) {
        MealOrder order = orderRepo.findById(orderId).orElse(null);
        
        // FIXED: Trim whitespace from mobile keyboard inputs
        String cleanOtp = inputOtp != null ? inputOtp.trim() : ""; 

        // FIXED: Added comprehensive null checks to prevent server crashes
        if (order != null && 
            order.getStatus() == OrderStatus.OUT_FOR_DELIVERY &&
            order.getDeliveryOtp() != null &&
            String.valueOf(order.getDeliveryOtp()).equals(cleanOtp) &&
            order.getDeliveryPartner() != null &&
            order.getDeliveryPartner().getId().equals(driver.getId())) {

            order.setStatus(OrderStatus.DELIVERED);
            orderRepo.save(order);

            User activeDriver = userRepo.findById(driver.getId()).orElse(driver);
            double currentBalance = (activeDriver.getWalletBalance() == null) ? 0.0 : activeDriver.getWalletBalance();
            
            activeDriver.setWalletBalance(currentBalance + 40.0); 
            userRepo.save(activeDriver);

            return true;
        }
        
        return false;
    }
}