package com.example.bai2.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(

        @NotNull
        Long senderAccountId,

        @NotBlank
        @Size(min = 6, max = 20)
        String receiverAccountNumber,

        @NotBlank
        @Pattern(regexp = "VCB|TCB|MB")
        String bankCode,

        @NotNull
        @DecimalMin(
                value = "10000",
                inclusive = false
        )
        BigDecimal amount,

        @Size(max = 255)
        String description
) {
}