package com.example.bai3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGRetrievalService {

    private final VectorStore vectorStore;

    @Value("${rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Value("${rag.top-k:3}")
    private int topK;

    public static final String NO_RELEVANT_DOCUMENT_MESSAGE =
            "Xin lỗi, thông tin bạn tìm kiếm không nằm trong tài liệu quy chế của chúng tôi.";

    public RetrievalResult retrieve(String query) {

        // =====================================================
        // 1. Validate query
        // =====================================================

        if (query == null || query.isBlank()) {

            log.warn("RAG BLOCKED | reason=EMPTY_QUERY");

            return RetrievalResult.blocked(
                    NO_RELEVANT_DOCUMENT_MESSAGE
            );
        }

        log.info(
                "RAG SEARCH | query='{}' | threshold={} | topK={}",
                query,
                similarityThreshold,
                topK
        );

        // =====================================================
        // 2. Vector search
        // =====================================================

        SearchRequest searchRequest =
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build();

        List<Document> documents;

        try {

            documents = vectorStore.similaritySearch(
                    searchRequest
            );

        } catch (Exception e) {

            log.error(
                    "RAG SEARCH ERROR | query='{}'",
                    query,
                    e
            );

            return RetrievalResult.blocked(
                    NO_RELEVANT_DOCUMENT_MESSAGE
            );
        }

        // =====================================================
        // 3. Không có document
        // =====================================================

        if (documents == null || documents.isEmpty()) {

            log.warn(
                    "RAG BLOCKED | reason=NO_RELEVANT_DOCUMENT"
            );

            return RetrievalResult.blocked(
                    NO_RELEVANT_DOCUMENT_MESSAGE
            );
        }

        // =====================================================
        // 4. Giới hạn Top K
        // =====================================================

        List<Document> relevantDocuments =
                documents.stream()
                        .limit(topK)
                        .toList();

        // =====================================================
        // 5. Không có document sau filter
        // =====================================================

        if (relevantDocuments.isEmpty()) {

            log.warn(
                    "RAG BLOCKED | reason=BELOW_THRESHOLD"
            );

            return RetrievalResult.blocked(
                    NO_RELEVANT_DOCUMENT_MESSAGE
            );
        }

        log.info(
                "RAG RETRIEVAL SUCCESS | documents={}",
                relevantDocuments.size()
        );

        return RetrievalResult.success(
                relevantDocuments
        );
    }

    public record RetrievalResult(
            boolean blocked,
            String message,
            List<Document> documents
    ) {

        public static RetrievalResult success(
                List<Document> documents
        ) {

            return new RetrievalResult(
                    false,
                    null,
                    documents
            );
        }

        public static RetrievalResult blocked(
                String message
        ) {

            return new RetrievalResult(
                    true,
                    message,
                    List.of()
            );
        }
    }
}