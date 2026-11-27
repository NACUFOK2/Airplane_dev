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

@SpringBootTest(properties = {"rag.llm.provider=extractive", "rag.feedback.log-dir=target/test-chat-logs"})
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    EmbeddingClient embeddingClient;

    @Test
    void listsTwentyOrganizationsWithCounts() throws Exception {
        mvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20))
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

    @Test
    void feedbackAndOutOfScopeShowUpInInsights() throws Exception {
        float[] v = new float[384];
        v[0] = 1f;
        when(embeddingClient.embedQuery(anyString())).thenReturn(Optional.of(v));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"insights-test 범위밖 질문\",\"orgCode\":\"ALL\"}"));
        mvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"down\",\"question\":\"insights-test 별로\",\"orgCode\":\"ALL\",\"mode\":\"extractive\",\"sourceIds\":[\"항공-014\"]}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.outOfScopeCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void rejectsInvalidFeedback() throws Exception {
        mvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"meh\",\"question\":\"q\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insightsForbiddenFromRemoteAddress() throws Exception {
        mvc.perform(get("/api/insights").with(r -> { r.setRemoteAddr("203.0.113.5"); return r; }))
                .andExpect(status().isForbidden());
    }

    @Test
    void outOfScopeAnswerListsAllOrganizationTypes() throws Exception {
        float[] v = new float[384];
        v[0] = 1f;
        when(embeddingClient.embedQuery(anyString())).thenReturn(Optional.of(v));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"김치찌개 끓이는 법\",\"orgCode\":\"ALL\"}"))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("숙박업체: 아고다"),
                        org.hamcrest.Matchers.containsString("ANA(전일본공수)"))));
    }

    @Test
    void validationMessagesAreKorean() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않아요."));
        mvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"q\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("평가 값이 없어요."));
    }
}
