package com.cloudlabcopilot.boot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("诊断 SSE 流式契约测试")
class DiagnosisStreamContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("事件序列含 stream_version/context/restate/final 且 streamVersion=1.0")
    void streamEventSequence() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/diagnosis/questions/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"relation does not exist\",\"techStack\":\"OPENGAUSS\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("event:stream_version");
        assertThat(body).contains("event:context");
        assertThat(body).contains("event:final");
        assertThat(body).contains("\"streamVersion\":\"1.0\"");
        assertThat(body).contains("\"seq\":");
    }

    @Test
    @DisplayName("next_step 整块事件含 safetyChecked=true")
    void nextStepSafetyChecked() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/diagnosis/questions/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"connection refused\",\"techStack\":\"OPENGAUSS\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("event:next_step");
        assertThat(body).contains("\"safetyChecked\":true");
    }
}