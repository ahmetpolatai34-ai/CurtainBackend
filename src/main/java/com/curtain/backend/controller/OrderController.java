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

    @GetMapping("/reports/performance")
    public java.util.Map<String, Object> getPerformanceMetrics() {
        return orderService.getPerformanceMetrics();
    }

    @GetMapping("/stats")
    public java.util.Map<String, Object> getStatistics() {
        return orderService.getStatistics();
    }

    @GetMapping("/{orderNumber}/logs")
    public List<com.curtain.backend.entity.WorkLog> getOrderLogs(@PathVariable String orderNumber) {
        return orderService.getOrderLogs(orderNumber);
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

    @PostMapping("/search")
    public List<Order> searchOrders(@RequestBody Map<String, Object> searchCriteria) {
        return orderService.searchOrders(searchCriteria);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        Order updatedOrder = orderService.updateOrder(id, order);
        if (updatedOrder != null) {
            return ResponseEntity.ok(updatedOrder);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{orderNumber}/issue")
    public ResponseEntity<Void> reportIssue(@PathVariable String orderNumber,
            @RequestBody java.util.Map<String, String> payload) {
        String reason = payload.get("reason");
        String worker = payload.get("worker");

        boolean success = orderService.reportIssue(orderNumber, reason, worker);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        boolean deleted = orderService.deleteOrder(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
