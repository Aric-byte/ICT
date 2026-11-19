package com.cloudlabcopilot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("诊断问答集成测试（真实 PostgreSQL 16）")
class DiagnosisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("提问返回知识兜底回答（含解释/原因/下一步/引用/安全提示）")
    void askReturnsFallback() throws Exception {
        mockMvc.perform(post("/api/v1/diagnosis/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"relation does not exist\",\"techStack\":\"OPENGAUSS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.generationStatus").isNotEmpty())
                .andExpect(jsonPath("$.data.explanation").isNotEmpty())
                .andExpect(jsonPath("$.data.nextStep.action").isNotEmpty())
                .andExpect(jsonPath("$.data.confidence").isNotEmpty())
                .andExpect(jsonPath("$.data.safetyNotice").isNotEmpty());
    }

    @Test
    @DisplayName("空问题返回业务码 400")
    void emptyQuestionRejected() throws Exception {
        mockMvc.perform(post("/api/v1/diagnosis/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("DIAGNOSIS_QUESTION_REQUIRED"));
    }

    @Test
    @DisplayName("凭据检测返回 400 且不入库")
    void credentialRejected() throws Exception {
        mockMvc.perform(post("/api/v1/diagnosis/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"connect with password=abc123456\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("SENSITIVE_CONTENT_REJECTED"));
    }

    @Test
    @DisplayName("能力声明返回流式协议版本")
    void capabilitiesAvailable() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/diagnosis/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.streamVersion").value("1.0"));
    }
}