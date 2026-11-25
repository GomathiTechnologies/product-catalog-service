package com.example.productcatalogservice;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EmbeddingControllerTest {
    private final EmbeddingModel model = mock(EmbeddingModel.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new EmbeddingController(new EmbeddingService(model, mock(PdfReader.class), new InMemoryVectorStore()))).build();

    @Test void returnsVectorForText() throws Exception {
        when(model.embed("Hello")).thenReturn(new float[] {0.5f, -0.25f});
        mvc.perform(post("/ai/embedding").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Hello\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dimensions").value(2))
                .andExpect(jsonPath("$.vector[0]").value(0.5)).andExpect(jsonPath("$.vector[1]").value(-0.25));
        verify(model).embed("Hello");
    }

    @Test void rejectsMissingOrBlankText() throws Exception {
        for (String body : new String[] {"{}", "{\"text\":\"  \"}"}) {
            mvc.perform(post("/ai/embedding").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(model);
    }

    @Test void handlesModelFailure() throws Exception {
        when(model.embed("Hello")).thenThrow(new IllegalStateException("sensitive details"));
        mvc.perform(post("/ai/embedding").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Hello\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sensitive details"))));
    }
}
