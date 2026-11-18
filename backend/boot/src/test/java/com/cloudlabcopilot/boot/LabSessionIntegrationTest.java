package com.cloudlabcopilot.boot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("实验-会话端到端集成测试（真实 PostgreSQL 16）")
class LabSessionIntegrationTest {

    private static final String TEACHER = "it-teacher";
    private static final String STUDENT = "it-student";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("创建实验→发布→开始会话→推进→上下文 全流程")
    void fullFlow() throws Exception {
        String labCode = "T-IT-" + System.currentTimeMillis();

        String createBody = """
                {"labCode":"%s","name":"IT实验","techStack":"MYSQL","difficulty":"BEGINNER",
                 "objective":"集成测试目标","estimatedMinutes":30,"tags":["it"]}
                """
                .formatted(labCode);
        mockMvc.perform(post("/api/v1/labs")
                .header("X-Operator-Id", TEACHER)
                .header("X-Operator-Role", "AUTHOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labCode").value(labCode));

        String stepsBody = """
                {"steps":[{"stepNo":1,"title":"连接","instruction":"mysql -u root","completionCriteria":"成功"},
                          {"stepNo":2,"title":"建表","instruction":"CREATE TABLE","completionCriteria":"成功"}]}
                """;
        mockMvc.perform(put("/api/v1/labs/" + labCode + "/steps")
                .header("X-Operator-Id", TEACHER)
                .header("X-Operator-Role", "AUTHOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepsBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String preBody = """
                {"preconditions":[{"sortNo":1,"type":"ENV","level":"REQUIRED","description":"已安装MySQL"}]}
                """;
        mockMvc.perform(put("/api/v1/labs/" + labCode + "/preconditions")
                .header("X-Operator-Id", TEACHER)
                .header("X-Operator-Role", "AUTHOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(preBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/labs/" + labCode + "/publish")
                .header("X-Operator-Id", TEACHER)
                .header("X-Operator-Role", "AUTHOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        MvcResult catalogResult = mockMvc.perform(get("/api/v1/labs")
                .header("X-Operator-Id", STUDENT)
                .header("X-Operator-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode catalog = objectMapper.readTree(catalogResult.getResponse().getContentAsString());
        assertTrue(catalog.path("data").path("total").asLong() >= 1);

        MvcResult detailResult = mockMvc.perform(get("/api/v1/labs/" + labCode)
                .header("X-Operator-Id", STUDENT)
                .header("X-Operator-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        long labId = detail.path("data").path("labId").asLong();
        int versionNo = detail.path("data").path("versionNo").asInt();
        assertTrue(labId > 0);
        assertEquals(1, versionNo);

        String startBody = "{\"labId\":" + labId + ",\"versionNo\":1,\"preconditionConfirmed\":true}";
        MvcResult startResult = mockMvc.perform(post("/api/v1/sessions")
                .header("X-Operator-Id", STUDENT)
                .header("X-Operator-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(startBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andReturn();
        JsonNode start = objectMapper.readTree(startResult.getResponse().getContentAsString());
        String sessionId = start.path("data").path("sessionId").asText();
        assertFalse(sessionId.isEmpty());

        mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/advance")
                .header("X-Operator-Id", STUDENT)
                .header("X-Operator-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectStepNo\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentStepNo").value(2));

        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/context")
                .header("X-Operator-Id", STUDENT)
                .header("X-Operator-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stateLabel").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentStepNo").value(2));
    }

    @Test
    @DisplayName("创建重复编码实验返回业务码 409")
    void duplicateLabCodeRejected() throws Exception {
        String labCode = "T-DUP-" + System.currentTimeMillis();
        String body = "{\"labCode\":\""
                + labCode
                + "\",\"name\":\"dup\",\"techStack\":\"MYSQL\",\"difficulty\":\"BEGINNER\",\"objective\":\"o\"}";
        mockMvc.perform(post("/api/v1/labs")
                .header("X-Operator-Id", TEACHER)
                .header("X-Operator-Role", "AUTHOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/v1/labs")
                .header("X-Operator-Id", TEACHER)
                .header("X-Operator-Role", "AUTHOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.errorCode").value("LAB_CODE_CONFLICT"));
    }
}