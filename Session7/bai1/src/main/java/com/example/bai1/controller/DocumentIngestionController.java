package com.example.bai1.controller;

import com.example.bai1.service.DocumentIngestionService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/documents")
public class DocumentIngestionController {

    private final DocumentIngestionService ingestionService;

    public DocumentIngestionController(
            DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) {

        try {

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("File không được để trống");
            }

            String fileName = file.getOriginalFilename();

            if (fileName == null ||
                    !fileName.toLowerCase().endsWith(".md")) {

                return ResponseEntity.badRequest()
                        .body("Chỉ hỗ trợ file Markdown (.md)");
            }

            Resource resource = file.getResource();

            int chunks = ingestionService.ingest(
                    resource,
                    category,
                    fileName
            );

            return ResponseEntity.ok(
                    "Ingest thành công. Số chunk: " + chunks
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body("Ingest thất bại: " + e.getMessage());
        }
    }
}