package com.example.bai2.config;


import com.example.bai2.splitter.HeaderBasedTextSplitter;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
public class ChunkingConfiguration {

    /**
     * Token-based Chunking
     *
     * Dùng cho tài liệu quy trình có nhiều bước.
     *
     * chunkSize = 450 token
     * minChunkSizeChars = 180 characters
     */
    @Bean("crmTokenTextSplitter")
    public TextSplitter crmTokenTextSplitter() {

        return TokenTextSplitter.builder()
                .withChunkSize(450)
                .withMinChunkSizeChars(180)
                .withMinChunkLengthToEmbed(20)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .withPunctuationMarks(
                        List.of(
                                '.',
                                '?',
                                '!',
                                '\n',
                                ';',
                                ':'
                        )
                )
                .build();
    }

    /**
     * Header-based Chunking
     *
     * Dùng cho tài liệu có cấu trúc:
     *
     * # Chương I
     * ## Điều 1
     * ## Điều 2
     *
     * Giữ lại heading để bảo toàn context.
     */
    @Bean("crmHeaderTextSplitter")
    public TextSplitter crmHeaderTextSplitter() {

        return new HeaderBasedTextSplitter(
                Set.of(1, 2),
                120
        );
    }
}
