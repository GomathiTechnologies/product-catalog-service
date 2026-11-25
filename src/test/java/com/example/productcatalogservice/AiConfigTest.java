package com.example.productcatalogservice;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiConfigTest {
    @Test void registersExecutableCallbacksForBothTools() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.defaultToolCallbacks(any(ToolCallback[].class))).thenReturn(builder);
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        new AiConfig().chatClient(builder, new CartTool(), new ProvinceProductsTool());
        var captor = ArgumentCaptor.forClass(ToolCallback[].class);
        verify(builder).defaultToolCallbacks(captor.capture());
        verify(builder).defaultSystem(contains("return its JSON result exactly as received"));
        ToolCallback[] callbacks = captor.getValue();
        assertThat(callbacks).extracting(callback -> callback.getToolDefinition().name())
            .containsExactlyInAnyOrder("addToCart", "getProvinceProducts");
        for (ToolCallback callback : callbacks) {
            if (callback.getToolDefinition().name().equals("addToCart")) {
                assertThat(callback.getToolMetadata().returnDirect()).isTrue();
                assertThat(callback.call("{\"productId\":\"internet-500\"}")).contains("internet-500");
            } else {
                assertThat(callback.getToolMetadata().returnDirect()).isTrue();
                assertThat(callback.call("{\"province\":\"NU\"}")).contains("No service available");
            }
        }
    }
}
