package com.curtain.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "ordering_store")
    private String orderingStore;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_tel")
    private String customerTel;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_address")
    private String customerAddress;

    @Column(name = "order_detail", columnDefinition = "TEXT")
    private String orderDetail;

    private Double amount;

    private LocalDate date;

    @Column(name = "current_station")
    private String currentStation; // Stores state_code

    @Column(name = "order_state")
    private String orderState; // IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "current_station_id")
    private Long currentStationId;
}
