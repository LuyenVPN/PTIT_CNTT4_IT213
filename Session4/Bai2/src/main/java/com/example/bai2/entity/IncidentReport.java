package com.example.bai2.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String driverName;

    @Column(nullable = false)
    private String vehicleCode;

    @Column(nullable = false)
    private String incidentType;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private String location;

    protected IncidentReport() {
        // Required by JPA
    }

    public IncidentReport(
            String driverName,
            String vehicleCode,
            String incidentType,
            String description,
            String location
    ) {
        this.driverName = driverName;
        this.vehicleCode = vehicleCode;
        this.incidentType = incidentType;
        this.description = description;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getVehicleCode() {
        return vehicleCode;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }
}