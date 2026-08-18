package com.example.bai2.mapper;

import com.example.bai2.dto.IncidentExtraction;
import com.example.bai2.entity.IncidentReport;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

    public IncidentReport toEntity(IncidentExtraction extraction) {

        if (extraction == null) {
            throw new IllegalArgumentException(
                    "Incident extraction must not be null"
            );
        }

        validate(extraction);

        return new IncidentReport(
                extraction.driverName(),
                extraction.vehicleCode(),
                extraction.incidentType(),
                extraction.description(),
                extraction.location()
        );
    }

    private void validate(IncidentExtraction extraction) {

        if (isBlank(extraction.driverName())) {
            throw new IllegalArgumentException(
                    "Driver name must not be blank"
            );
        }

        if (isBlank(extraction.vehicleCode())) {
            throw new IllegalArgumentException(
                    "Vehicle code must not be blank"
            );
        }

        if (isBlank(extraction.incidentType())) {
            throw new IllegalArgumentException(
                    "Incident type must not be blank"
            );
        }

        if (isBlank(extraction.description())) {
            throw new IllegalArgumentException(
                    "Description must not be blank"
            );
        }

        if (isBlank(extraction.location())) {
            throw new IllegalArgumentException(
                    "Location must not be blank"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}