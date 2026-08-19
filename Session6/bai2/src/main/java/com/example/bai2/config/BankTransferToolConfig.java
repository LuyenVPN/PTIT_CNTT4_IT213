package com.example.bai2.config;

import com.example.bai2.dto.TransferRequest;
import com.example.bai2.dto.TransferResponse;
import com.example.bai2.dto.TransferStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Function;

@Configuration
public class BankTransferToolConfig {

    /**
     * Giả lập số dư tài khoản nguồn trong Core Banking.
     */
    private static final BigDecimal AVAILABLE_BALANCE =
            new BigDecimal("5000000");

    /**
     * Tool chuyển khoản ngân hàng.
     *
     * Input:
     * TransferRequest
     *
     * Output:
     * TransferResponse
     */
    @Bean
    public Function<TransferRequest, TransferResponse> bankTransferTool() {

        return request -> {

            System.out.println("===== BANK TRANSFER TOOL =====");
            System.out.println(
                    "Sender Account ID: "
                            + request.senderAccountId()
            );
            System.out.println(
                    "Receiver Account Number: "
                            + request.receiverAccountNumber()
            );
            System.out.println(
                    "Bank Code: "
                            + request.bankCode()
            );
            System.out.println(
                    "Amount: "
                            + request.amount()
            );

            // Kiểm tra số dư giả lập
            if (request.amount().compareTo(AVAILABLE_BALANCE) > 0) {

                System.out.println(
                        "Transfer FAILED: Insufficient balance"
                );

                return new TransferResponse(
                        null,
                        TransferStatus.FAILED,
                        "Insufficient balance. "
                                + "Available balance: "
                                + AVAILABLE_BALANCE
                                + " VND"
                );
            }

            // Giả lập giao dịch thành công
            String transactionId =
                    "TXN-" + UUID.randomUUID();

            System.out.println(
                    "Transfer SUCCESS"
            );

            System.out.println(
                    "Transaction ID: "
                            + transactionId
            );

            return new TransferResponse(
                    transactionId,
                    TransferStatus.SUCCESS,
                    "Transfer completed successfully"
            );
        };
    }
}
