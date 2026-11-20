package com.example.productcatalogservice;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProvinceProductsControllerTest {
    private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new ProvinceProductsController(new ProvinceProductsTool())).build();

    @Test void returnsOntarioPlans() throws Exception {
        mvc.perform(get("/dynamic/province/products").param("province", " ontario "))
            .andExpect(status().isOk()).andExpect(jsonPath("$.province").value("Ontario"))
            .andExpect(jsonPath("$.products.length()").value(3))
            .andExpect(jsonPath("$.products[0].name").value("Ontario Super Save 1Gbps"))
            .andExpect(jsonPath("$.products[0].downloadSpeedMbps").value(1000));
    }

    @Test void returnsQuebecPlans() throws Exception {
        mvc.perform(get("/dynamic/province/products").param("province", "Québec"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.products.length()").value(3))
            .andExpect(jsonPath("$.products[0].name").value("French Frenzy 5Gbps"))
            .andExpect(jsonPath("$.products[0].downloadSpeedMbps").value(5000));
    }

    @Test void handlesNunavutAndInvalidInput() throws Exception {
        mvc.perform(get("/dynamic/province/products").param("province", "NU"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("No service available"))
            .andExpect(jsonPath("$.products.length()").value(0));
        for (String input : new String[] {"", "Atlantis"}) {
            mvc.perform(get("/dynamic/province/products").param("province", input)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/dynamic/province/products")).andExpect(status().isBadRequest());
    }

    @Test void supportsAllOtherProvincesAndTerritories() throws Exception {
        for (String code : new String[] {"AB", "BC", "MB", "NB", "NL", "NS", "PE", "SK", "NT", "YT"}) {
            mvc.perform(get("/dynamic/province/products").param("province", code))
                .andExpect(status().isOk()).andExpect(jsonPath("$.products.length()").value(3));
        }
    }
}
