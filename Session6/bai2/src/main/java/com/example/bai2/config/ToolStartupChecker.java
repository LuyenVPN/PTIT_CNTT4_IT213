package com.example.bai2.config;

import com.example.bai2.dto.TransferRequest;
import com.example.bai2.dto.TransferResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class ToolStartupChecker {

    @Bean
    public CommandLineRunner checkBankTransferTool(
            ApplicationContext applicationContext
    ) {

        return args -> {

            Function<TransferRequest, TransferResponse> tool =
                    applicationContext.getBean(
                            "bankTransferTool",
                            Function.class
                    );

            System.out.println(
                    "========================================"
            );
            System.out.println(
                    "Spring AI Tool Registration Check"
            );
            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "Tool name: bankTransferTool"
            );

            System.out.println(
                    "Bean type: "
                            + tool.getClass().getName()
            );

            System.out.println(
                    "STATUS: bankTransferTool registered successfully!"
            );

            System.out.println(
                    "========================================"
            );
        };
    }
}