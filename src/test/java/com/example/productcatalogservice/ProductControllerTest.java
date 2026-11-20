package com.example.productcatalogservice;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ProductControllerTest {
    @Test void returnsSampleInternetProduct() throws Exception {
        MockMvcBuilders.standaloneSetup(new ProductController(new ProductService())).build()
            .perform(get("/api/products")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("internet-500"))
            .andExpect(jsonPath("$[0].downloadSpeedMbps").value(500))
            .andExpect(jsonPath("$[0].monthlyPrice").value(59.99))
            .andExpect(jsonPath("$[0].currency").value("CAD"));
    }
}
