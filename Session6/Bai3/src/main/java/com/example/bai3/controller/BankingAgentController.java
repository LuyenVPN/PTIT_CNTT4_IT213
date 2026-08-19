package com.example.bai3.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agent")
public class BankingAgentController {

    private final ChatClient chatClient;
    private final ToolCallback bankTransferTool;

    public BankingAgentController(
            ChatClient.Builder chatClientBuilder,
            ToolCallback bankTransferTool
    ) {

        this.bankTransferTool = bankTransferTool;

        this.chatClient = chatClientBuilder
                .build();
    }

    @PostMapping("/chat")
    public String chat(
            @RequestParam String prompt
    ) {

        return chatClient
                .prompt()
                .system("""
                        You are RikkeiPay Banking Assistant.

                        You help users with banking operations.

                        When the user explicitly requests a bank transfer
                        and provides enough information, use the
                        bankTransferTool.

                        Required information:
                        - sender account
                        - receiver account number
                        - bank
                        - amount
                        - transfer description

                        Do not invent missing transaction information.

                        After the tool execution, clearly communicate
                        the transaction result to the user.
                        """)
                .user(prompt)
                .tools(bankTransferTool)
                .call()
                .content();
    }
}
