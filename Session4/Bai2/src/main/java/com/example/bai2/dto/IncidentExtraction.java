package com.example.bai2.dto;

public record IncidentExtraction(
        String driverName,
        String vehicleCode,
        String incidentType,
        String description,
        String location
) {
}
