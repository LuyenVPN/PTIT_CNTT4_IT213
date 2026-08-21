package com.example.bai3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final RAGRetrievalService retrievalService;

    private final ChatClient chatClient;

    public String ask(String question) {

        log.info(
                "USER QUESTION | '{}'",
                question
        );

        // =====================================================
        // 1. Retrieval
        // =====================================================

        RAGRetrievalService.RetrievalResult result =
                retrievalService.retrieve(question);

        // =====================================================
        // 2. BLOCK
        //
        // Quan trọng:
        // Không gọi ChatClient ở nhánh này.
        // =====================================================

        if (result.blocked()) {

            log.warn(
                    "LLM NOT CALLED | reason=NO_RELEVANT_CONTEXT"
            );

            return result.message();
        }

        // =====================================================
        // 3. Build context
        // =====================================================

        String context =
                result.documents()
                        .stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n\n"));

        log.info(
                "CONTEXT CREATED | documents={}",
                result.documents().size()
        );

        // =====================================================
        // 4. Gọi LLM
        // =====================================================

        log.info(
                "LLM CALLED"
        );

        return chatClient
                .prompt()
                .system("""
                        Bạn là trợ lý CRM.

                        NHIỆM VỤ:

                        - Chỉ được trả lời dựa trên CONTEXT.
                        - Không được tự tạo thông tin doanh nghiệp.
                        - Không được suy đoán chính sách.
                        - Nếu CONTEXT không chứa câu trả lời,
                          hãy nói rằng thông tin không có trong tài liệu.

                        CONTEXT:
                        %s
                        """.formatted(context))
                .user(question)
                .call()
                .content();
    }
}