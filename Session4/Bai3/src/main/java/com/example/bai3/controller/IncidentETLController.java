package com.example.bai3.controller;

import com.example.bai3.entity.IncidentReport;
import com.example.bai3.service.IncidentETLService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentETLController {

    private final IncidentETLService etlService;

    public IncidentETLController(
            IncidentETLService etlService
    ) {
        this.etlService = etlService;
    }

    @PostMapping("/etl")
    public IncidentReport process(
            @RequestBody String rawMessage
    ) {

        return etlService.processReport(rawMessage);
    }
}