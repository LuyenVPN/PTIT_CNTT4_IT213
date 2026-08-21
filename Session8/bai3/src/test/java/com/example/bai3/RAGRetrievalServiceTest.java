package com.example.bai3;

import com.example.bai3.service.RAGRetrievalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RAGRetrievalServiceTest {

    @Mock
    private VectorStore vectorStore;

    private RAGRetrievalService retrievalService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        retrievalService =
                new RAGRetrievalService(vectorStore);

        setField(
                retrievalService,
                "similarityThreshold",
                0.75
        );

        setField(
                retrievalService,
                "topK",
                3
        );
    }

    // =========================================================
    // TEST 1
    // Empty query
    // =========================================================

    @Test
    void shouldBlockEmptyQuery() {

        RAGRetrievalService.RetrievalResult result =
                retrievalService.retrieve("");

        assertTrue(
                result.blocked()
        );

        assertEquals(
                RAGRetrievalService.NO_RELEVANT_DOCUMENT_MESSAGE,
                result.message()
        );

        assertTrue(
                result.documents().isEmpty()
        );

        verify(
                vectorStore,
                never()
        ).similaritySearch(
                any(SearchRequest.class)
        );
    }

    // =========================================================
    // TEST 2
    // VectorStore không trả document
    // =========================================================

    @Test
    void shouldBlockWhenNoDocumentsFound() {

        when(
                vectorStore.similaritySearch(
                        any(SearchRequest.class)
                )
        ).thenReturn(
                List.of()
        );

        RAGRetrievalService.RetrievalResult result =
                retrievalService.retrieve(
                        "Làm thế nào để học Java?"
                );

        assertTrue(
                result.blocked()
        );

        assertEquals(
                RAGRetrievalService.NO_RELEVANT_DOCUMENT_MESSAGE,
                result.message()
        );

        assertTrue(
                result.documents().isEmpty()
        );

        verify(
                vectorStore,
                times(1)
        ).similaritySearch(
                any(SearchRequest.class)
        );
    }

    // =========================================================
    // TEST 3
    // SearchRequest phải có threshold = 0.75
    // =========================================================

    @Test
    void shouldUseConfiguredSimilarityThreshold() {

        Document document =
                new Document(
                        "Quy định nghỉ phép của nhân viên."
                );

        when(
                vectorStore.similaritySearch(
                        any(SearchRequest.class)
                )
        ).thenReturn(
                List.of(document)
        );

        retrievalService.retrieve(
                "Quy định nghỉ phép?"
        );

        ArgumentCaptor<SearchRequest> captor =
                ArgumentCaptor.forClass(
                        SearchRequest.class
                );

        verify(
                vectorStore
        ).similaritySearch(
                captor.capture()
        );

        SearchRequest request =
                captor.getValue();

        assertEquals(
                0.75,
                request.getSimilarityThreshold()
        );
    }

    // =========================================================
    // TEST 4
    // SearchRequest phải có topK = 3
    // =========================================================

    @Test
    void shouldUseConfiguredTopK() {

        Document doc1 =
                new Document("Document 1");

        Document doc2 =
                new Document("Document 2");

        Document doc3 =
                new Document("Document 3");

        when(
                vectorStore.similaritySearch(
                        any(SearchRequest.class)
                )
        ).thenReturn(
                List.of(
                        doc1,
                        doc2,
                        doc3
                )
        );

        retrievalService.retrieve(
                "Quy định CRM"
        );

        ArgumentCaptor<SearchRequest> captor =
                ArgumentCaptor.forClass(
                        SearchRequest.class
                );

        verify(
                vectorStore
        ).similaritySearch(
                captor.capture()
        );

        SearchRequest request =
                captor.getValue();

        assertEquals(
                3,
                request.getTopK()
        );
    }

    // =========================================================
    // TEST 5
    // Không được vượt quá Top K
    // =========================================================

    @Test
    void shouldReturnMaximumThreeDocuments() {

        Document doc1 =
                new Document("Document 1");

        Document doc2 =
                new Document("Document 2");

        Document doc3 =
                new Document("Document 3");

        Document doc4 =
                new Document("Document 4");

        Document doc5 =
                new Document("Document 5");

        when(
                vectorStore.similaritySearch(
                        any(SearchRequest.class)
                )
        ).thenReturn(
                List.of(
                        doc1,
                        doc2,
                        doc3,
                        doc4,
                        doc5
                )
        );

        RAGRetrievalService.RetrievalResult result =
                retrievalService.retrieve(
                        "Quy định CRM"
                );

        assertFalse(
                result.blocked()
        );

        assertEquals(
                3,
                result.documents().size()
        );
    }

    // =========================================================
    // TEST 6
    // Retrieval thành công
    // =========================================================

    @Test
    void shouldReturnDocumentsWhenFound() {

        Document document =
                new Document(
                        "Quy định nghỉ phép của nhân viên."
                );

        when(
                vectorStore.similaritySearch(
                        any(SearchRequest.class)
                )
        ).thenReturn(
                List.of(document)
        );

        RAGRetrievalService.RetrievalResult result =
                retrievalService.retrieve(
                        "Quy định nghỉ phép?"
                );

        assertFalse(
                result.blocked()
        );

        assertNull(
                result.message()
        );

        assertEquals(
                1,
                result.documents().size()
        );
    }

    // =========================================================
    // Reflection helper
    // =========================================================

    private void setField(
            Object object,
            String fieldName,
            Object value
    ) {

        try {

            var field =
                    object.getClass()
                            .getDeclaredField(fieldName);

            field.setAccessible(true);

            field.set(
                    object,
                    value
            );

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}