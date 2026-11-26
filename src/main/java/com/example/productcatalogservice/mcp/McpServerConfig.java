package com.example.productcatalogservice.mcp;

import com.example.productcatalogservice.CartTool;
import com.example.productcatalogservice.ProvinceProductsTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {
    @Bean
    ToolCallbackProvider catalogMcpTools(CartTool cartTool, ProvinceProductsTool provinceProductsTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(cartTool, provinceProductsTool)
                .build();
    }
}
