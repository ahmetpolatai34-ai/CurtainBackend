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
        }
        return orderRepository.save(order);
    }
    
    @Transactional
    public boolean processOrderStation(String orderNumber, Long stationId, String workerUsername) {
         Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
         if (orderOpt.isEmpty()) return false;
         
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
                if (order.getDate() != null && order.getDate().toString().compareTo(dateFrom) < 0) matches = false;
            }
            
            String dateTo = getSafeString(criteria, "dateTo");
            if (dateTo != null) {
                if (order.getDate() != null && order.getDate().toString().compareTo(dateTo) > 0) matches = false;
            }

            // Amount filtering
            String amountMinStr = getSafeString(criteria, "amountMin");
            if (amountMinStr != null) {
                 try {
                    double min = Double.parseDouble(amountMinStr);
                    if (order.getAmount() == null || order.getAmount() < min) matches = false;
                 } catch (NumberFormatException e) { }
            }
            
            String amountMaxStr = getSafeString(criteria, "amountMax");
            if (amountMaxStr != null) {
                 try {
                     double max = Double.parseDouble(amountMaxStr);
                     if (order.getAmount() == null || order.getAmount() > max) matches = false;
                 } catch (NumberFormatException e) { }
            }

            // String partial matches (Case-insensitive)
            String orderNumber = getSafeString(criteria, "orderNumber");
            if (orderNumber != null) {
                if (order.getOrderNumber() == null || !order.getOrderNumber().toLowerCase().contains(orderNumber.toLowerCase())) matches = false;
            }
            
            String orderingStore = getSafeString(criteria, "orderingStore");
            if (orderingStore != null) {
                if (order.getOrderingStore() == null || !order.getOrderingStore().toLowerCase().contains(orderingStore.toLowerCase())) matches = false;
            }
            
            String customerName = getSafeString(criteria, "customerName");
            if (customerName != null) {
                if (order.getCustomerName() == null || !order.getCustomerName().toLowerCase().contains(customerName.toLowerCase())) matches = false;
            }
            
            String customerTel = getSafeString(criteria, "customerTel");
            if (customerTel != null) {
                if (order.getCustomerTel() == null || !order.getCustomerTel().contains(customerTel)) matches = false;
            }
            
            String customerEmail = getSafeString(criteria, "customerEmail");
            if (customerEmail != null) {
                if (order.getCustomerEmail() == null || !order.getCustomerEmail().toLowerCase().contains(customerEmail.toLowerCase())) matches = false;
            }

            // State filtering
            String state = getSafeString(criteria, "state");
            if (state != null) {
                 if (order.getCurrentStation() == null || !order.getCurrentStation().toLowerCase().contains(state.toLowerCase())) {
                     matches = false;
                 }
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
}
