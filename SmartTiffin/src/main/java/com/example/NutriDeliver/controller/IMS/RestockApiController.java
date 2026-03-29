package com.example.NutriDeliver.controller.IMS;

import com.example.NutriDeliver.dto.RestockRequestDTO;
import com.example.NutriDeliver.service.IMS.RestockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class RestockApiController {

    @Autowired
    private RestockService restockService;

    @PostMapping("/restock")
    public ResponseEntity<?> receiveRestock(
            @RequestParam("hubId") Long hubId, // FIX: Catching the Hub ID from the frontend URL
            @RequestBody RestockRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        try {
            String username = "Admin";
            // FIX: Passing hubId into the service layer
            restockService.processRestock(request, idempotencyKey, username, hubId);
            return ResponseEntity.ok().body("Restock processed successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
        }
    }
}