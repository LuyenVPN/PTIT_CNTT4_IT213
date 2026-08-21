package com.example.bai2.controller;

import com.example.bai2.service.ChunkingDemoService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chunking")
public class ChunkingDemoController {

    private final ChunkingDemoService chunkingDemoService;

    public ChunkingDemoController(
            ChunkingDemoService chunkingDemoService) {

        this.chunkingDemoService =
                chunkingDemoService;
    }

    @PostMapping("/token")
    public List<String> tokenChunking(
            @RequestBody String content) {

        return chunkingDemoService
                .splitByToken(content)
                .stream()
                .map(Document::getText)
                .toList();
    }

    @PostMapping("/header")
    public List<String> headerChunking(
            @RequestBody String content) {

        return chunkingDemoService
                .splitByHeader(content)
                .stream()
                .map(Document::getText)
                .toList();
    }
}
