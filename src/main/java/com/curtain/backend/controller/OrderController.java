package com.curtain.backend.controller;

import com.curtain.backend.entity.Order;
import com.curtain.backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }
    
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<Order> getOrderByBarcode(@PathVariable String barcode) {
        Order order = orderService.getOrderByNumber(barcode);
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    @PostMapping("/process")
    public ResponseEntity<?> processStation(@RequestBody Map<String, Object> payload) {
        String barcode = (String) payload.get("barcode");
        Long stationId = ((Number) payload.get("stationId")).longValue();
        String worker = (String) payload.get("worker");
        
        boolean success = orderService.processOrderStation(barcode, stationId, worker);
        if (success) {
            return ResponseEntity.ok().body("{\"message\": \"Station completed\"}");
        } else {
            return ResponseEntity.badRequest().body("{\"message\": \"Failed to process station\"}");
        }
    }
}
