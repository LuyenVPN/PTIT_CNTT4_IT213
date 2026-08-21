package com.example.bai3.controller;

import com.example.bai3.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class ChatController {

    private final RAGService ragService;

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(
            @RequestBody ChatRequest request
    ) {

        if (request == null ||
                request.question() == null ||
                request.question().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(
                            new ChatResponse(
                                    "Vui lòng nhập câu hỏi."
                            )
                    );
        }

        String answer =
                ragService.ask(request.question());

        return ResponseEntity.ok(
                new ChatResponse(answer)
        );
    }

    public record ChatRequest(
            String question
    ) {
    }

    public record ChatResponse(
            String answer
    ) {
    }
}