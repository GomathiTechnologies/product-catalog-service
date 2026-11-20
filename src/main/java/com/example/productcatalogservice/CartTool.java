package com.example.productcatalogservice;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CartTool {
    @Tool(description = "Create a sample order containing the requested product. This does not persist a cart or place an order.")
    public SampleOrder addToCart(@ToolParam(description = "The product ID to add") String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId is required");
        }
        var item = new OrderItem(productId, "Sample Internet Plan", new BigDecimal("59.99"), "CAD");
        return new SampleOrder(UUID.randomUUID().toString(), List.of(item), item.price(), item.currency());
    }

    public record OrderItem(String productId, String name, BigDecimal price, String currency) {}
    public record SampleOrder(String orderId, List<OrderItem> items, BigDecimal total, String currency) {}
}
