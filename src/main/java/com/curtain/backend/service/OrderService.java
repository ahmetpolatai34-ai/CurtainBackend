package com.curtain.backend.service;

import com.curtain.backend.entity.Order;
import com.curtain.backend.entity.Station;
import com.curtain.backend.entity.WorkLog;
import com.curtain.backend.repository.OrderRepository;
import com.curtain.backend.repository.StationRepository;
import com.curtain.backend.repository.WorkLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private WorkLogRepository workLogRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByDateRange(String startDateStr, String endDateStr) {
        LocalDate start = (startDateStr != null && !startDateStr.isEmpty()) ? LocalDate.parse(startDateStr) : null;
        LocalDate end = (endDateStr != null && !endDateStr.isEmpty()) ? LocalDate.parse(endDateStr) : null;

        if (start != null && end != null) {
            return orderRepository.findByDateBetween(start, end);
        } else if (start != null) {
            return orderRepository.findByDateGreaterThanEqual(start);
        } else if (end != null) {
            return orderRepository.findByDateLessThanEqual(end);
        }
        return orderRepository.findAll();
    }

    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).orElse(null);
    }

    public Order createOrder(Order order) {
        // Set initial station (lowest step)
        List<Station> stations = stationRepository.findAllByOrderByStepAsc();
        if (!stations.isEmpty()) {
            order.setCurrentStationId(stations.get(0).getId());
            order.setCurrentStation(stations.get(0).getStateCode()); // Initial status code
            order.setOrderState("IN_PROGRESS");
        } else {
            order.setOrderState("PENDING");
        }

        Order savedOrder = orderRepository.save(order);

        // Only create log if station exists to avoid null constraint violation in
        // WorkLog
        if (savedOrder.getCurrentStationId() != null) {
            WorkLog log = new WorkLog();
            log.setOrderId(savedOrder.getId());
            log.setStationId(savedOrder.getCurrentStationId());
            log.setWorkerUsername("SYSTEM");
            log.setAction("ORDER_CREATED");
            workLogRepository.save(log);
        }

        return savedOrder;
    }

    @Transactional
    public boolean processOrderStation(String orderNumber, Long stationId, String workerUsername) {
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty())
            return false;

        Order order = orderOpt.get();

        // Verify we are at the correct station (optional strict check)
        // For now, we assume if the worker scans it, they are doing the work.

        // Log the work
        WorkLog log = new WorkLog();
        log.setOrderId(order.getId());
        log.setStationId(stationId);
        log.setWorkerUsername(workerUsername);
        workLogRepository.save(log);

        // Move to next station
        List<Station> stations = stationRepository.findAllByOrderByStepAsc();
        Station currentStation = stationRepository.findById(stationId).orElse(null);

        if (currentStation != null) {
            boolean foundCurrent = false;
            Station nextStation = null;

            for (Station s : stations) {
                if (foundCurrent) {
                    nextStation = s;
                    break;
                }
                if (s.getId().equals(stationId)) {
                    foundCurrent = true;
                }
            }

            if (nextStation != null) {
                order.setCurrentStationId(nextStation.getId());
                order.setCurrentStation(nextStation.getStateCode());
            } else {
                order.setOrderState("COMPLETED"); // End of line
                order.setCurrentStationId(null);
                // We keep the last currentStation code or set to "COMPLETED"?
                // Request implies state_code link. If completed, maybe no station?
                // Let's keep the last one or set explicit string.
                order.setCurrentStation("COMPLETED");
            }
            orderRepository.save(order);
            return true;
        }

        return false;
    }

    @Transactional
    public boolean reportIssue(String orderNumber, String reason, String workerUsername) {
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty())
            return false;

        Order order = orderOpt.get();
        order.setIsBlocked(true);
        order.setBlockReason(reason);
        // We might want to change state to BLOCKED as well, or keep IN_PROGRESS but
        // paused
        order.setOrderState("BLOCKED");

        // Log the issue
        WorkLog log = new WorkLog();
        log.setOrderId(order.getId());
        log.setStationId(order.getCurrentStationId());
        log.setWorkerUsername(workerUsername);
        log.setAction("ISSUE_REPORTED: " + reason); // Assuming we add 'action' field or reuse existing
        // Since WorkLog currently doesn't have 'action', we might need to modify
        // WorkLog or just save it.
        // Let's modify WorkLog entity first to support 'action' or 'notes'.
        // For now, I'll assume WorkLog only has basic fields and skip saving custom
        // notes there
        // unless I update WorkLog. Let's update WorkLog first.

        orderRepository.save(order);
        return true;
    }

    public Order updateOrder(Long id, Order orderDetails) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            order.setOrderingStore(orderDetails.getOrderingStore());
            order.setDeliveryAddress(orderDetails.getDeliveryAddress());
            order.setCustomerName(orderDetails.getCustomerName());
            order.setCustomerTel(orderDetails.getCustomerTel());
            order.setCustomerEmail(orderDetails.getCustomerEmail());
            order.setCustomerAddress(orderDetails.getCustomerAddress());
            order.setOrderDetail(orderDetails.getOrderDetail());
            order.setDate(orderDetails.getDate());
            order.setAmount(orderDetails.getAmount());
            // We usually don't update orderNumber or currentStation via simple update
            return orderRepository.save(order);
        }
        return null;
    }

    public boolean deleteOrder(Long id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private String getSafeString(java.util.Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            String val = map.get(key).toString();
            if (!val.trim().isEmpty()) {
                return val;
            }
        }
        return null;
    }

    public List<Order> searchOrders(java.util.Map<String, Object> criteria) {
        List<Order> allOrders = orderRepository.findAll();

        return allOrders.stream().filter(order -> {
            boolean matches = true;

            // Date filtering
            String dateFrom = getSafeString(criteria, "dateFrom");
            if (dateFrom != null) {
                if (order.getDate() != null && order.getDate().toString().compareTo(dateFrom) < 0)
                    matches = false;
            }

            String dateTo = getSafeString(criteria, "dateTo");
            if (dateTo != null) {
                if (order.getDate() != null && order.getDate().toString().compareTo(dateTo) > 0)
                    matches = false;
            }

            // Amount filtering
            String amountMinStr = getSafeString(criteria, "amountMin");
            if (amountMinStr != null) {
                try {
                    double min = Double.parseDouble(amountMinStr);
                    if (order.getAmount() == null || order.getAmount() < min)
                        matches = false;
                } catch (NumberFormatException e) {
                }
            }

            String amountMaxStr = getSafeString(criteria, "amountMax");
            if (amountMaxStr != null) {
                try {
                    double max = Double.parseDouble(amountMaxStr);
                    if (order.getAmount() == null || order.getAmount() > max)
                        matches = false;
                } catch (NumberFormatException e) {
                }
            }

            // String partial matches (Case-insensitive)
            String orderNumber = getSafeString(criteria, "orderNumber");
            if (orderNumber != null) {
                if (order.getOrderNumber() == null
                        || !order.getOrderNumber().toLowerCase().contains(orderNumber.toLowerCase()))
                    matches = false;
            }

            String orderingStore = getSafeString(criteria, "orderingStore");
            if (orderingStore != null) {
                if (order.getOrderingStore() == null
                        || !order.getOrderingStore().toLowerCase().contains(orderingStore.toLowerCase()))
                    matches = false;
            }

            String customerName = getSafeString(criteria, "customerName");
            if (customerName != null) {
                if (order.getCustomerName() == null
                        || !order.getCustomerName().toLowerCase().contains(customerName.toLowerCase()))
                    matches = false;
            }

            String customerTel = getSafeString(criteria, "customerTel");
            if (customerTel != null) {
                if (order.getCustomerTel() == null || !order.getCustomerTel().contains(customerTel))
                    matches = false;
            }

            String customerEmail = getSafeString(criteria, "customerEmail");
            if (customerEmail != null) {
                if (order.getCustomerEmail() == null
                        || !order.getCustomerEmail().toLowerCase().contains(customerEmail.toLowerCase()))
                    matches = false;
            }

            // State filtering
            String state = getSafeString(criteria, "state");
            if (state != null) {
                boolean stateMatch = false;

                // 1. Check current station (e.g., CUTTING, SEWING)
                if (order.getCurrentStation() != null &&
                        order.getCurrentStation().toLowerCase().contains(state.toLowerCase())) {
                    stateMatch = true;
                }

                // 2. Check general order state (COMPLETED, BLOCKED, CANCELLED)
                if (order.getOrderState() != null &&
                        order.getOrderState().toLowerCase().contains(state.toLowerCase())) {
                    stateMatch = true;
                }

                // 3. Explicit check for isBlocked property
                if ("BLOCKED".equalsIgnoreCase(state) && Boolean.TRUE.equals(order.getIsBlocked())) {
                    stateMatch = true;
                }

                if (!stateMatch)
                    matches = false;
            }

            // Exclude Completed
            if (criteria.containsKey("excludeCompleted") && criteria.get("excludeCompleted") != null) {
                if (Boolean.parseBoolean(criteria.get("excludeCompleted").toString())) {
                    if ("COMPLETED".equalsIgnoreCase(order.getOrderState())) {
                        matches = false;
                    }
                }
            }

            return matches;
        }).collect(java.util.stream.Collectors.toList());
    }

    public List<WorkLog> getOrderLogs(String orderNumber) {
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isPresent()) {
            return workLogRepository.findByOrderIdOrderByTimestampAsc(orderOpt.get().getId());
        }
        return java.util.Collections.emptyList();
    }

    public java.util.Map<String, Object> getStatistics(String startDate, String endDate) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        List<Order> filteredOrders = getOrdersByDateRange(startDate, endDate);

        long totalOrders = filteredOrders.size();
        long completed = filteredOrders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getOrderState())).count();
        long inProgress = filteredOrders.stream().filter(o -> "IN_PROGRESS".equalsIgnoreCase(o.getOrderState()))
                .count();
        long cancelled = filteredOrders.stream().filter(o -> "CANCELLED".equalsIgnoreCase(o.getOrderState())).count();
        long blocked = filteredOrders.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsBlocked()) || "BLOCKED".equalsIgnoreCase(o.getOrderState()))
                .count();

        stats.put("totalOrders", totalOrders);
        stats.put("completed", completed);
        stats.put("inProgress", inProgress);
        stats.put("cancelled", cancelled);
        stats.put("blocked", blocked);

        // Extension: Breakdown by station
        java.util.Map<String, Long> stationCounts = filteredOrders.stream()
                .filter(o -> o.getCurrentStation() != null && "IN_PROGRESS".equalsIgnoreCase(o.getOrderState()))
                .collect(java.util.stream.Collectors.groupingBy(Order::getCurrentStation,
                        java.util.stream.Collectors.counting()));
        stats.put("stationCounts", stationCounts);

        return stats;
    }

    public java.util.Map<String, Object> getPerformanceMetrics() {
        java.util.Map<String, Object> metrics = new java.util.HashMap<>();
        List<Order> completedOrders = orderRepository.findAll().stream()
                .filter(o -> "COMPLETED".equalsIgnoreCase(o.getOrderState()))
                .collect(java.util.stream.Collectors.toList());

        // 1. Average Completion Time
        if (!completedOrders.isEmpty()) {
            double totalHours = 0;
            int count = 0;
            for (Order order : completedOrders) {
                List<WorkLog> logs = workLogRepository.findByOrderIdOrderByTimestampAsc(order.getId());
                // Find creation time (first log) and completion time (last log)
                // This is an estimation. Ideally order has createdDate.
                // We use first log as proxy for start if logs exist.
                if (logs.size() >= 2) {
                    java.time.LocalDateTime start = logs.get(0).getTimestamp();
                    java.time.LocalDateTime end = logs.get(logs.size() - 1).getTimestamp();
                    long minutes = java.time.Duration.between(start, end).toMinutes();
                    totalHours += (minutes / 60.0);
                    count++;
                }
            }
            if (count > 0) {
                metrics.put("avgCompletionTime", String.format("%.2f", totalHours / count));
            } else {
                metrics.put("avgCompletionTime", "0.0");
            }
        } else {
            metrics.put("avgCompletionTime", "0.0");
        }

        // 2. Bottleneck Station (Station with most logs/time spent?)
        // Simplification: Station where orders sit the longest?
        // Or simply station with most current orders?
        List<Order> activeOrders = orderRepository.findAll().stream()
                .filter(o -> "IN_PROGRESS".equalsIgnoreCase(o.getOrderState()))
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<String, Long> stationLoad = activeOrders.stream()
                .filter(o -> o.getCurrentStation() != null)
                .collect(java.util.stream.Collectors.groupingBy(Order::getCurrentStation,
                        java.util.stream.Collectors.counting()));

        if (!stationLoad.isEmpty()) {
            java.util.Map.Entry<String, Long> maxEntry = java.util.Collections.max(stationLoad.entrySet(),
                    java.util.Map.Entry.comparingByValue());
            metrics.put("bottleneckStation", maxEntry.getKey());
            metrics.put("bottleneckCount", maxEntry.getValue());
        } else {
            metrics.put("bottleneckStation", null);
            metrics.put("bottleneckCount", 0);
        }

        // 3. Fastest Order
        String fastestOrderNo = "N/A";
        long minMinutes = Long.MAX_VALUE;

        for (Order order : completedOrders) {
            List<WorkLog> logs = workLogRepository.findByOrderIdOrderByTimestampAsc(order.getId());
            if (logs.size() >= 2) {
                java.time.LocalDateTime start = logs.get(0).getTimestamp();
                java.time.LocalDateTime end = logs.get(logs.size() - 1).getTimestamp();
                long minutes = java.time.Duration.between(start, end).toMinutes();

                if (minutes < minMinutes) {
                    minMinutes = minutes;
                    fastestOrderNo = order.getOrderNumber();
                }
            }
        }

        if (minMinutes != Long.MAX_VALUE) {
            metrics.put("fastestOrder", fastestOrderNo + " (" + minMinutes + " min)");
        } else {
            metrics.put("fastestOrder", "N/A");
        }

        return metrics;
    }
}
