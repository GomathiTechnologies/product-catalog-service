package com.example.productcatalogservice;

import org.springframework.ai.chat.client.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ai")
public class AiChatController {
    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);
    private final ChatClient chatClient;

    public AiChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        log.info("Received request at /ai/chat");
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }
        try {
            String response = chatClient.prompt().user(request.prompt()).call().content();
            if (response == null) throw new IllegalStateException("Empty model response");
            return new ChatResponse(response);
        } catch (RuntimeException exception) {
            log.warn("AI chat request failed ({}).", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI chat is temporarily unavailable");
        }
    }

    public record ChatRequest(String prompt) {}
    public record ChatResponse(String response) {}
}
