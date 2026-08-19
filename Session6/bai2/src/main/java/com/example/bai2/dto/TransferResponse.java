package com.example.bai2.dto;

public record TransferResponse(
        String transactionId,
        TransferStatus status,
        String message
) {
}