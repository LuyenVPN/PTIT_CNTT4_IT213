package com.example.bai1.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/incident")
public class SystemConfigController {

    @Value("${spring.profiles.active:unknown}")
    private String activeProfile;

    @Value("${app.llm.model}")
    private String llmModel;

    @Value("${app.llm.provider}")
    private String llmProvider;

    @GetMapping("/config")
    public Map<String, String> getSystemConfig() {

        return Map.of(
                "application", "AI Logistics Incident Reporter",
                "activeProfile", activeProfile,
                "llmProvider", llmProvider,
                "llmModel", llmModel
        );
    }
}