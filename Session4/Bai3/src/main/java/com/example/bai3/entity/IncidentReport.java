package com.example.bai3.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderCode;

    @Column(nullable = false)
    private String licensePlate;

    @Column(nullable = false)
    private String incidentType;

    @Column(nullable = false)
    private String urgency;

    protected IncidentReport() {
        // Required by JPA
    }

    public IncidentReport(
            String orderCode,
            String licensePlate,
            String incidentType,
            String urgency
    ) {
        this.orderCode = orderCode;
        this.licensePlate = licensePlate;
        this.incidentType = incidentType;
        this.urgency = urgency;
    }

    public Long getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public String getUrgency() {
        return urgency;
    }
}