package com.example.bai3.config;

import com.example.bai3.dto.TransferRequest;
import com.example.bai3.dto.TransferResponse;
import com.example.bai3.dto.TransferStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Function;

@Configuration
public class BankTransferToolConfig {

    private static final BigDecimal AVAILABLE_BALANCE =
            new BigDecimal("5000000");

    @Bean
    public ToolCallback bankTransferTool() {

        Function<TransferRequest, TransferResponse> function =
                request -> {

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

                    // Kiểm tra số dư
                    if (request.amount()
                            .compareTo(AVAILABLE_BALANCE) > 0) {

                        System.out.println(
                                "Transfer FAILED: Insufficient balance"
                        );

                        return new TransferResponse(
                                null,
                                TransferStatus.FAILED,
                                "Insufficient balance. Available balance: "
                                        + AVAILABLE_BALANCE
                                        + " VND"
                        );
                    }

                    // Sinh transaction ID
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

        return FunctionToolCallback
                .builder(
                        "bankTransferTool",
                        function
                )
                .description("""
                        Transfer money from the authenticated user's
                        source bank account to a recipient bank account.

                        Use this tool only when the user explicitly
                        requests a bank transfer.

                        Parameters:
                        - senderAccountId: source account ID.
                        - receiverAccountNumber: destination account number.
                        - bankCode: destination bank code such as VCB, TCB or MB.
                        - amount: transfer amount in Vietnamese Dong (VND).
                        - description: transfer description.

                        The tool checks whether the source account
                        has sufficient balance.

                        If the balance is sufficient, the transfer
                        succeeds and a unique transaction ID is returned.

                        If the balance is insufficient, the transfer
                        returns FAILED.
                        """)
                .inputType(TransferRequest.class)
                .build();
    }
}