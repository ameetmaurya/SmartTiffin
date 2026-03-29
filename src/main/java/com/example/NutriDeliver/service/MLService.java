package com.example.NutriDeliver.service;

import com.example.NutriDeliver.model.MealOrder;
import com.example.NutriDeliver.model.OrderStatus;
import com.example.NutriDeliver.model.User;
import com.example.NutriDeliver.repository.OrderRepository;
import com.example.NutriDeliver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MLService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * AI Spatial Clustering Algorithm for Delivery Allocation
     * Groups orders by location and balances them across available drivers.
     */
    @Transactional
    public Map<String, Object> runClusteringAllocation() {
        // 1. Fetch all PACKED orders waiting at the Hub
        List<MealOrder> pendingDispatches = orderRepository.findByStatus(OrderStatus.PACKED);

        if (pendingDispatches.isEmpty()) {
            throw new IllegalStateException("No PACKED orders available for allocation.");
        }

        // 2. Fetch all available Delivery Partners
        // FIX: Changed "DELIVERY_PARTNER" String to User.Role.DELIVERY_PARTNER enum
        List<User> drivers = userRepository.findByRole(User.Role.DELIVERY_PARTNER);
        if (drivers.isEmpty()) {
            throw new IllegalStateException("No Delivery Partners available in the system.");
        }

        // 3. ML Heuristic: Group orders spatially by Hostel Cluster
        Map<String, List<MealOrder>> spatialClusters = pendingDispatches.stream()
                .collect(Collectors.groupingBy(MealOrder::getHostelCluster));

        // 4. Sort clusters by density (largest number of orders first)
        List<Map.Entry<String, List<MealOrder>>> sortedClusters = new ArrayList<>(spatialClusters.entrySet());
        sortedClusters.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));

        // 5. Smart Load Balancing Allocation
        Map<String, Integer> allocationResults = new HashMap<>();
        int driverIndex = 0;

        for (Map.Entry<String, List<MealOrder>> cluster : sortedClusters) {
            String location = cluster.getKey();
            List<MealOrder> ordersInCluster = cluster.getValue();

            // Select the next driver (Round-Robin for load balancing)
            User assignedDriver = drivers.get(driverIndex % drivers.size());

            // Bulk Assign
            for (MealOrder order : ordersInCluster) {
                order.setDeliveryPartner(assignedDriver);
                order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
            }

            // Record results for the UI
            String driverLog = assignedDriver.getFullName() + " (" + location + ")";
            allocationResults.put(driverLog, allocationResults.getOrDefault(driverLog, 0) + ordersInCluster.size());

            driverIndex++;
        }

        orderRepository.saveAll(pendingDispatches);

        // Prepare response summary
        Map<String, Object> response = new HashMap<>();
        response.put("totalOrdersAllocated", pendingDispatches.size());
        response.put("clustersProcessed", sortedClusters.size());
        response.put("driverAssignments", allocationResults);

        return response;
    }
}