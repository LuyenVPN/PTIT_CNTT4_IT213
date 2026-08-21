package com.example.bai2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Bai2Application {

    private static final Logger log =
            LoggerFactory.getLogger(
                    Bai2Application.class
            );

    public static void main(String[] args) {

        SpringApplication.run(
                Bai2Application.class,
                args
        );
    }

    @Bean
    CommandLineRunner verifyChunkingBeans(
            ApplicationContext context) {

        return args -> {

            TextSplitter tokenSplitter =
                    context.getBean(
                            "crmTokenTextSplitter",
                            TextSplitter.class
                    );

            TextSplitter headerSplitter =
                    context.getBean(
                            "crmHeaderTextSplitter",
                            TextSplitter.class
                    );

            log.info(
                    "=== CRM CHUNKING BEAN VERIFICATION ==="
            );

            log.info(
                    "Registered bean: crmTokenTextSplitter -> {}",
                    tokenSplitter
                            .getClass()
                            .getSimpleName()
            );

            log.info(
                    "Registered bean: crmHeaderTextSplitter -> {}",
                    headerSplitter
                            .getClass()
                            .getSimpleName()
            );

            log.info(
                    "Spring Context verification: SUCCESS"
            );
        };
    }

}
