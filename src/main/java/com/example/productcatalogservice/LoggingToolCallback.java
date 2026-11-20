package com.example.productcatalogservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** Logs only recognized sample identifiers, never arbitrary model-supplied text. */
final class LoggingToolCallback implements ToolCallback {
    private static final Logger log = LoggerFactory.getLogger(LoggingToolCallback.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> REGIONS = Set.of("on", "qc", "ab", "bc", "mb", "nb", "nl", "ns", "pe", "sk", "nt", "yt", "nu", "ontario", "quebec", "québec", "alberta", "british columbia", "manitoba", "new brunswick", "newfoundland and labrador", "nova scotia", "prince edward island", "saskatchewan", "northwest territories", "yukon", "nunavut");
    private final ToolCallback delegate;

    LoggingToolCallback(ToolCallback delegate) { this.delegate = delegate; }
    @Override public ToolDefinition getToolDefinition() { return delegate.getToolDefinition(); }
    @Override public ToolMetadata getToolMetadata() { return delegate.getToolMetadata(); }
    @Override public String call(String input) {
        logInput(input);
        return delegate.call(input);
    }
    @Override public String call(String input, ToolContext context) {
        logInput(input);
        return delegate.call(input, context);
    }

    private void logInput(String input) {
        String name = getToolDefinition().name();
        String parameter = name.equals("getProvinceProducts") ? "province" : name.equals("addToCart") ? "productId" : "input";
        String value = "[redacted]";
        try {
            JsonNode node = JSON.readTree(input).path(parameter);
            if (node.isTextual()) {
                String candidate = node.textValue().strip().toLowerCase(Locale.ROOT);
                if (parameter.equals("province") && REGIONS.contains(candidate)) value = candidate;
                if (parameter.equals("productId") && (candidate.equals("internet-500") || candidate.matches("(?:on|qc|ab|bc|mb|nb|nl|ns|pe|sk|nt|yt)-(?:super|essential|basic)"))) value = candidate;
            }
        } catch (Exception ignored) {
            // Malformed or unexpected input is never logged.
        }
        log.info("Executing AI tool {} with {}={}", name, parameter, value);
    }
}
