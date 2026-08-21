package com.example.bai1.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentIngestionService {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Transactional
    public int ingest(Resource resource,
                      String category,
                      String sourceFile) {

        try {
            if (resource == null || !resource.exists()) {
                throw new IllegalArgumentException(
                        "File không tồn tại: " + sourceFile
                );
            }

            if (!sourceFile.toLowerCase().endsWith(".md")) {
                throw new IllegalArgumentException(
                        "Chỉ hỗ trợ file Markdown (.md)"
                );
            }

            log.info("Bắt đầu ingest file: {}", sourceFile);

            MarkdownDocumentReaderConfig config =
                    MarkdownDocumentReaderConfig.builder()
                            .withAdditionalMetadata(
                                    Map.of(
                                            "category", category,
                                            "source_file", sourceFile
                                    )
                            )
                            .build();

            MarkdownDocumentReader reader =
                    new MarkdownDocumentReader(resource, config);

            List<Document> documents = reader.read();

            TokenTextSplitter splitter =
                    TokenTextSplitter.builder()
                            .withChunkSize(600)
                            .withMinChunkSizeChars(120)
                            .withMinChunkLengthToEmbed(10)
                            .withMaxNumChunks(10_000)
                            .withKeepSeparator(true)
                            .build();

            List<Document> chunks =
                    splitter.apply(documents);

            if (chunks.isEmpty()) {
                log.warn("Không có chunk để embedding: {}", sourceFile);
                return 0;
            }

            vectorStore.add(chunks);

            log.info(
                    "Ingest thành công: file={}, category={}, documents={}, chunks={}",
                    sourceFile,
                    category,
                    documents.size(),
                    chunks.size()
            );

            return chunks.size();

        } catch (Exception e) {

            log.error(
                    "Lỗi ingest file {}: {}",
                    sourceFile,
                    e.getMessage(),
                    e
            );

            throw new RuntimeException(
                    "Không thể ingest tài liệu: " + sourceFile,
                    e
            );
        }
    }
}