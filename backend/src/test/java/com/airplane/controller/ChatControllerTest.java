package com.airplane.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "rag.llm.provider=extractive")
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listsAirlines() throws Exception {
        mvc.perform(get("/api/airlines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].code").value("KE"));
    }

    @Test
    void answersWithSourcesFromSelectedAirline() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"위탁 수하물 몇 kg까지 무료예요?\",\"airlineCode\":\"7C\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("extractive"))
                .andExpect(jsonPath("$.sources[0].airlineCode").value("7C"))
                .andExpect(jsonPath("$.sources[0].category").value("수하물"));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\",\"airlineCode\":\"KE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void unknownAirlineReturns404() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"수하물\",\"airlineCode\":\"ZZ\"}"))
                .andExpect(status().isNotFound());
    }
}
