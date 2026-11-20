package com.example.productcatalogservice;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiChatControllerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiChatController()).build();

    @Test void returnsSampleResponseForPrompt() throws Exception {
        mvc.perform(post("/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"Hello\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value("Sample response to: Hello"));
    }

    @Test void rejectsMissingOrBlankPrompt() throws Exception {
        for (String body : new String[] {"{}", "{\"prompt\":\"  \"}"}) {
            mvc.perform(post("/ai/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
    }
}
