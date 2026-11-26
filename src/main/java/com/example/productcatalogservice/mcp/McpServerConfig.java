package com.example.productcatalogservice.mcp;

import com.example.productcatalogservice.CartTool;
import com.example.productcatalogservice.ProvinceProductsTool;
import com.example.productcatalogservice.schedule.InstallationScheduleTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {
    @Bean
    ToolCallbackProvider catalogMcpTools(CartTool cartTool, ProvinceProductsTool provinceProductsTool,
            InstallationScheduleTool installationScheduleTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(cartTool, provinceProductsTool, installationScheduleTool)
                .build();
    }
}
