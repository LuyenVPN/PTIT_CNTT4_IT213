package com.example.bai3.service;

import com.example.bai3.dto.IncidentExtraction;
import com.example.bai3.entity.IncidentReport;
import com.example.bai3.repository.IncidentRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IncidentETLService {

    private final ChatModel chatModel;
    private final IncidentRepository repository;

    private static final Set<String> VALID_URGENCY =
            Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private static final Pattern LICENSE_PLATE_PATTERN =
            Pattern.compile(
                    "^[0-9]{2}[A-Z]{1,2}-[0-9]{3}\\.?[0-9]{2}$"
            );

    public IncidentETLService(
            @Qualifier("ollamaChatModel") ChatModel chatModel,
            IncidentRepository repository
    ) {
        this.chatModel = chatModel;
        this.repository = repository;
    }

    @Transactional
    public IncidentReport processReport(String rawMessage) {

        log.info(
                "Starting incident ETL. Raw message length={}",
                rawMessage == null ? 0 : rawMessage.length()
        );

        try {

            // 1. Kiểm tra input
            if (rawMessage == null || rawMessage.isBlank()) {
                throw new IllegalArgumentException(
                        "Raw incident message must not be blank"
                );
            }

            // 2. Tạo BeanOutputConverter
            BeanOutputConverter<IncidentExtraction> converter =
                    new BeanOutputConverter<>(
                            IncidentExtraction.class
                    );

            // Spring AI 2.0.0 sử dụng getFormat()
            String formatInstructions =
                    converter.getFormat();

            // 3. Tạo Prompt
            String promptText =
                    """
                    Phân tích tin nhắn sự cố của tài xế dưới đây.

                    Tin nhắn:
                    %s

                    Hãy trả về JSON đúng theo format sau:
                    %s
                    """.formatted(
                            rawMessage,
                            formatInstructions
                    );

            Prompt prompt = new Prompt(promptText);

            // 4. Gọi Ollama
            String response =
                    chatModel
                            .call(prompt)
                            .getResult()
                            .getOutput()
                            .getText();

            log.info(
                    "AI response received successfully. Response length={}",
                    response == null ? 0 : response.length()
            );

            // 5. Làm sạch Markdown
            String cleanedJson =
                    cleanMarkdownJson(response);

            log.debug(
                    "Cleaned AI JSON: {}",
                    cleanedJson
            );

            // 6. Parse JSON -> DTO
            IncidentExtraction dto =
                    converter.convert(cleanedJson);

            log.info(
                    "AI extraction parsed successfully. " +
                            "orderCode={}, licensePlate={}, urgency={}",
                    dto.orderCode(),
                    dto.licensePlate(),
                    dto.urgency()
            );

            // 7. Defensive Validation
            validateExtraction(dto);

            // 8. DTO -> Entity
            IncidentReport entity =
                    new IncidentReport(
                            dto.orderCode(),
                            dto.licensePlate(),
                            dto.incidentType(),
                            dto.urgency()
                    );

            // 9. Lưu database
            IncidentReport saved =
                    repository.save(entity);

            log.info(
                    "Incident saved successfully. id={}, orderCode={}",
                    saved.getId(),
                    saved.getOrderCode()
            );

            return saved;

        } catch (Exception ex) {

            log.error(
                    "Incident ETL failed. " +
                            "rawMessageLength={}, error={}",
                    rawMessage == null ? 0 : rawMessage.length(),
                    ex.getMessage(),
                    ex
            );

            // Ném lại exception để Transactional rollback
            throw ex;
        }
    }

    /**
     * Loại bỏ Markdown code block:
     *
     * ```json
     * {...}
     * ```
     *
     * thành:
     *
     * {...}
     */
    private String cleanMarkdownJson(String response) {

        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException(
                    "AI response is empty"
            );
        }

        String cleaned = response.trim();

        // Loại bỏ ```json hoặc ```
        cleaned = cleaned.replaceFirst(
                "^```(?:json)?\\s*",
                ""
        );

        // Loại bỏ ``` ở cuối
        cleaned = cleaned.replaceFirst(
                "\\s*```$",
                ""
        );

        return cleaned.trim();
    }

    /**
     * Defensive validation.
     */
    private void validateExtraction(
            IncidentExtraction dto
    ) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Incident extraction must not be null"
            );
        }

        // orderCode
        if (dto.orderCode() == null
                || dto.orderCode().isBlank()) {

            throw new IllegalArgumentException(
                    "orderCode must not be blank"
            );
        }

        // licensePlate
        if (dto.licensePlate() == null
                || dto.licensePlate().isBlank()) {

            throw new IllegalArgumentException(
                    "licensePlate must not be blank"
            );
        }

        if (!LICENSE_PLATE_PATTERN.matcher(
                dto.licensePlate()
        ).matches()) {

            throw new IllegalArgumentException(
                    "Invalid Vietnamese license plate format: "
                            + dto.licensePlate()
            );
        }

        // incidentType
        if (dto.incidentType() == null
                || dto.incidentType().isBlank()) {

            throw new IllegalArgumentException(
                    "incidentType must not be blank"
            );
        }

        // urgency
        if (dto.urgency() == null
                || dto.urgency().isBlank()) {

            throw new IllegalArgumentException(
                    "urgency must not be blank"
            );
        }

        String urgency =
                dto.urgency().toUpperCase();

        if (!VALID_URGENCY.contains(urgency)) {

            throw new IllegalArgumentException(
                    "Invalid urgency: "
                            + dto.urgency()
                            + ". Allowed values: "
                            + VALID_URGENCY
            );
        }

        log.info(
                "Defensive validation passed. " +
                        "orderCode={}, licensePlate={}",
                dto.orderCode(),
                dto.licensePlate()
        );
    }
}