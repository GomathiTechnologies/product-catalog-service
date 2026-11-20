package com.example.productcatalogservice;

import java.util.Arrays;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, CartTool cartTool, ProvinceProductsTool provinceProductsTool) {
        ToolCallback[] callbacks = Arrays.stream(ToolCallbacks.from(cartTool, provinceProductsTool))
                .map(LoggingToolCallback::new).toArray(ToolCallback[]::new);
        return builder.defaultToolCallbacks(callbacks)
                .defaultSystem("""
                    When a tool is executed, return its JSON result exactly as received.
                    Do not summarize, reformat, or modify the data.
                    Do not include markdown.
                    """)
                .build();
    }
}
