package com.example.bai3.dto;

public record IncidentExtraction(
        String orderCode,
        String licensePlate,
        String incidentType,
        String urgency
) {
}