package com.example.bai1.dto;

public record TransferResponse(
        String transactionId,
        TransferStatus status,
        String message
) {
}