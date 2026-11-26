package com.example.productcatalogservice.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InstallationScheduleControllerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new InstallationScheduleController(new InstallationScheduleTool())).build();

    @Test void returnsSampleConfirmation() throws Exception {
        mvc.perform(post("/schedule/installation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2030-05-20\",\"time\":\"14:30\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.date").value("2030-05-20"))
                .andExpect(jsonPath("$.time").value("14:30"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No appointment has been booked")));
    }

    @Test void rejectsMissingAndInvalidDateOrTime() throws Exception {
        for (String body : new String[]{"{}", "{\"date\":\"2030-02-30\",\"time\":\"14:30\"}",
                "{\"date\":\"2030-05-20\",\"time\":\"24:00\"}",
                "{\"date\":\"2030-05-20\",\"time\":\"2:30\"}"}) {
            mvc.perform(post("/schedule/installation").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
