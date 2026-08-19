package com.example.bai4.dto;

public record TransferResponse(
        String transactionId,
        TransferStatus status,
        String message
) {
}