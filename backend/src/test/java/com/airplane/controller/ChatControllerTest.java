package com.airplane.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airplane.client.EmbeddingClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "rag.llm.provider=extractive")
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    EmbeddingClient embeddingClient;

    @Test
    void listsSevenOrganizationsWithCounts() throws Exception {
        mvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].name").value("하나투어"))
                .andExpect(jsonPath("$[0].qaCount").value(10));
    }

    @Test
    void answersFromSelectedOrgWithSourceUrl() throws Exception {
        when(embeddingClient.embedQuery(anyString())).thenReturn(Optional.empty());
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"위탁 수하물 몇 kg까지 무료예요?\",\"orgCode\":\"jejuair\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("extractive"))
                .andExpect(jsonPath("$.retrieval").value("bm25"))
                .andExpect(jsonPath("$.sources[0].org").value("제주항공"))
                .andExpect(jsonPath("$.sources[0].url").value(org.hamcrest.Matchers.startsWith("https://")))
                .andExpect(jsonPath("$.sources[0].official").value(true));
    }

    @Test
    void outOfScopeSkipsLlm() throws Exception {
        float[] v = new float[384];
        v[0] = 1f;
        when(embeddingClient.embedQuery(anyString())).thenReturn(Optional.of(v));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"오늘 날씨 어때\",\"orgCode\":\"ALL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("out_of_scope"))
                .andExpect(jsonPath("$.sources.length()").value(0));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\",\"orgCode\":\"ALL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void unknownOrgReturns404() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"수하물\",\"orgCode\":\"asiana\"}"))
                .andExpect(status().isNotFound());
    }
}
