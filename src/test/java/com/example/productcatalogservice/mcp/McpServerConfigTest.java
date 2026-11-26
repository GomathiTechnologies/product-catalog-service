package com.example.productcatalogservice.mcp;

import com.example.productcatalogservice.CartTool;
import com.example.productcatalogservice.ProvinceProductsTool;
import com.example.productcatalogservice.schedule.InstallationScheduleTool;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;

class McpServerConfigTest {
    @Test
    void exposesExecutableCatalogTools() {
        ToolCallback[] tools = new McpServerConfig()
                .catalogMcpTools(new CartTool(), new ProvinceProductsTool(), new InstallationScheduleTool()).getToolCallbacks();
        assertThat(tools).extracting(tool -> tool.getToolDefinition().name())
                .containsExactlyInAnyOrder("addToCart", "getProvinceProducts", "scheduleInstallation");
        ToolCallback cart = Arrays.stream(tools).filter(t -> t.getToolDefinition().name().equals("addToCart")).findFirst().orElseThrow();
        ToolCallback province = Arrays.stream(tools).filter(t -> t.getToolDefinition().name().equals("getProvinceProducts")).findFirst().orElseThrow();
        ToolCallback schedule = Arrays.stream(tools).filter(t -> t.getToolDefinition().name().equals("scheduleInstallation")).findFirst().orElseThrow();
        assertThat(schedule.call("{\"date\":\"2030-05-20\",\"time\":\"14:30\"}")).contains("2030-05-20", "14:30", "true");
        assertThat(cart.call("{\"productId\":\"internet-500\"}")).contains("internet-500");
        assertThat(province.call("{\"province\":\"NU\"}")).contains("No service available");
    }
}
