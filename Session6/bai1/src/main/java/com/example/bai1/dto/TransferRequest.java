package com.example.bai1.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(

        @NotNull(message = "senderAccountId không được để trống")
        Long senderAccountId,

        @NotBlank(message = "Số tài khoản người nhận không được để trống")
        @Size(
                min = 6,
                max = 20,
                message = "Số tài khoản người nhận phải từ 6 đến 20 ký tự"
        )
        String receiverAccountNumber,

        @NotBlank(message = "Mã ngân hàng không được để trống")
        @Pattern(
                regexp = "VCB|TCB|MB",
                message = "Mã ngân hàng không hợp lệ. Chỉ chấp nhận VCB, TCB hoặc MB"
        )
        String bankCode,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(
                value = "10000",
                inclusive = false,
                message = "Số tiền chuyển phải lớn hơn 10.000 VND"
        )
        BigDecimal amount,

        @Size(
                max = 255,
                message = "Nội dung chuyển khoản không được vượt quá 255 ký tự"
        )
        String description
) {
}
