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
            order.setCurrentStation(stations.get(0).getId());
            order.setStatus(stations.get(0).getStatus()); // Initial status
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
                 order.setCurrentStation(nextStation.getId());
                 order.setStatus(nextStation.getStatus());
             } else {
                 order.setStatus("COMPLETED"); // End of line
                 order.setCurrentStation(null);
             }
             orderRepository.save(order);
             return true;
         }
         
         return false;
    }
}
