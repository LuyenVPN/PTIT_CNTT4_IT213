package com.example.bai4.controller;

import com.example.bai4.dto.AgentChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agent")
public class BankingAgentController {

    private final ChatClient chatClient;
    private final ToolCallback bankTransferTool;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;

    public BankingAgentController(
            ChatClient.Builder chatClientBuilder,
            ToolCallback bankTransferTool,
            MessageChatMemoryAdvisor chatMemoryAdvisor
    ) {

        this.bankTransferTool = bankTransferTool;
        this.chatMemoryAdvisor = chatMemoryAdvisor;

        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "demo-user") String conversationId
    ) {

        String result = chatClient
                .prompt()
                .system("""
                        Bạn là RikkeiPay Banking Assistant, một trợ lý
                        giao dịch ngân hàng cẩn trọng và chính xác.

                        Nhiệm vụ của bạn là hỗ trợ người dùng thực hiện
                        chuyển khoản thông qua bankTransferTool.

                        BẮT BUỘC phải có đầy đủ các thông tin sau trước
                        khi gọi Tool:

                        1. senderAccountId
                        2. receiverAccountNumber
                        3. bankCode
                        4. amount

                        Nếu thiếu bất kỳ thông tin nào:
                        - Tuyệt đối không gọi bankTransferTool.
                        - Không được tự suy đoán.
                        - Hỏi người dùng thông tin còn thiếu.

                        Phải sử dụng thông tin đã được cung cấp ở các lượt
                        hội thoại trước đó.

                        Chỉ gọi bankTransferTool khi cả 4 thông tin bắt buộc
                        đã đầy đủ và hợp lệ.

                        Quy đổi ngân hàng:
                        - Vietcombank -> VCB
                        - Techcombank -> TCB
                        - MB Bank -> MB

                        Quy đổi số tiền:
                        - 100k = 100000 VND
                        - 200k = 200000 VND
                        - 1 triệu = 1000000 VND

                        Không bao giờ bịa thông tin giao dịch.

                        Sau khi Tool thực hiện xong, hãy thông báo rõ kết quả
                        giao dịch cho người dùng.
                        """)
                .advisors(advisor ->
                        advisor
                                .param(
                                        ChatMemory.CONVERSATION_ID,
                                        conversationId
                                )
                )
                .advisors(chatMemoryAdvisor)
                .user(prompt)
                .tools(bankTransferTool)
                .call()
                .content();

        return new AgentChatResponse(result);
    }
}
