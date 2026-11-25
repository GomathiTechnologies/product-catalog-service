package com.example.productcatalogservice;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KnowledgeChatControllerTest {
    private final EmbeddingModel embedding = mock(EmbeddingModel.class);
    private final ChatModel chat = mock(ChatModel.class);
    private final InMemoryVectorStore store = new InMemoryVectorStore();
    private final EmbeddingService service = new EmbeddingService(embedding, mock(PdfReader.class), store);
    private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(new KnowledgeChatController(service, chat)).build();

    @Test void answersUsingRetrievedContextWithoutToolCallbacks() throws Exception {
        store.replaceAll(List.of(new VectorDocument("id", "Static IP is available.", Map.of("file_name", "ip.pdf", "page_number", 1), new float[] {1, 0})));
        when(embedding.embed("fixed IP?")).thenReturn(new float[] {1, 0});
        when(chat.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Yes.")))));
        mvc.perform(post("/ai/knowledge/chat").contentType(MediaType.APPLICATION_JSON).content("{\"prompt\":\"fixed IP?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response").value("Yes."))
                .andExpect(jsonPath("$.sources[0].fileName").value("ip.pdf"));
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chat).call(prompt.capture());
        assertThat(prompt.getValue().getContents()).contains("Static IP is available.", "fixed IP?");
    }

    @Test void validatesAndReportsEmptyStoreWithoutCallingModels() throws Exception {
        mvc.perform(post("/ai/knowledge/chat").contentType(MediaType.APPLICATION_JSON).content("{\"prompt\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/ai/knowledge/chat").contentType(MediaType.APPLICATION_JSON).content("{\"prompt\":\"hello\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("empty")));
        verifyNoInteractions(embedding);
        verify(chat, never()).call(any(Prompt.class));
    }
}
