package com.example.bai4.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor chatMemoryAdvisor(
            ChatMemory chatMemory
    ) {
        return MessageChatMemoryAdvisor
                .builder(chatMemory)
                .build();
    }
}