package com.example.bai2.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChunkingDemoService {

    private final TextSplitter tokenSplitter;
    private final TextSplitter headerSplitter;

    public ChunkingDemoService(
            @Qualifier("crmTokenTextSplitter")
            TextSplitter tokenSplitter,

            @Qualifier("crmHeaderTextSplitter")
            TextSplitter headerSplitter) {

        this.tokenSplitter = tokenSplitter;
        this.headerSplitter = headerSplitter;
    }

    public List<Document> splitByToken(
            String content) {

        Document document =
                new Document(content);

        return tokenSplitter.apply(
                List.of(document)
        );
    }

    public List<Document> splitByHeader(
            String content) {

        Document document =
                new Document(content);

        return headerSplitter.apply(
                List.of(document)
        );
    }
}
