package com.example.bai2.controller;

import com.example.bai2.dto.IncidentExtraction;
import com.example.bai2.entity.IncidentReport;
import com.example.bai2.mapper.IncidentMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentMapper incidentMapper;

    public IncidentController(IncidentMapper incidentMapper) {
        this.incidentMapper = incidentMapper;
    }

    @PostMapping("/extract")
    public IncidentReport extract(
            @RequestBody IncidentExtraction extraction
    ) {

        return incidentMapper.toEntity(extraction);
    }
}