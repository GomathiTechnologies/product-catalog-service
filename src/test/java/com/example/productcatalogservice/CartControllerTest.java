package com.example.productcatalogservice;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartControllerTest {
    private final org.springframework.test.web.servlet.MockMvc mvc =
            MockMvcBuilders.standaloneSetup(new CartController(new CartTool())).build();

    @Test void returnsSampleOrderWithRequestedProduct() throws Exception {
        mvc.perform(post("/api/cart/add").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"productId":"internet-500"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").isNotEmpty())
            .andExpect(jsonPath("$.items[0].productId").value("internet-500"))
            .andExpect(jsonPath("$.items[0].name").value("Sample Internet Plan"))
            .andExpect(jsonPath("$.items[0].price").value(59.99))
            .andExpect(jsonPath("$.total").value(59.99))
            .andExpect(jsonPath("$.currency").value("CAD"));
    }

    @Test void rejectsMissingOrBlankProductId() throws Exception {
        for (String body : new String[] {"{}", "{\"productId\":\"  \"}"}) {
            mvc.perform(post("/api/cart/add").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
    }
}
