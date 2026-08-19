package com.example.bai3.dto;

public record TransferResponse(
        String transactionId,
        TransferStatus status,
        String message
) {
}