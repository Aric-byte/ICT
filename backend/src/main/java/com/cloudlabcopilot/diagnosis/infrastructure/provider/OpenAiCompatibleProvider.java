package com.cloudlabcopilot.diagnosis.infrastructure.provider;

import com.cloudlabcopilot.diagnosis.domain.port.ChatProviderPort;
import com.cloudlabcopilot.diagnosis.infrastructure.config.DiagnosisProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "cloudlab.diagnosis.provider.type", havingValue = "OPENAI_COMPATIBLE")
public class OpenAiCompatibleProvider implements ChatProviderPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);

    private final DiagnosisProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleProvider(DiagnosisProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getProvider().getSyncTimeoutMs()))
                .build();
    }

    @Override
    public String providerType() {
        return "OPENAI_COMPATIBLE";
    }

    @Override
    public boolean isConfigured() {
        DiagnosisProperties.Provider p = properties.getProvider();
        return p.getEndpointUrl() != null
                && !p.getEndpointUrl().contains("待核实")
                && !p.getEndpointUrl().isBlank()
                && p.getApiKey() != null
                && !p.getApiKey().isBlank();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (!isConfigured()) {
            throw new ProviderException("AI_UNCONFIGURED", "OpenAI 兼容端点或 API Key 未配置");
        }

        long start = System.currentTimeMillis();
        DiagnosisProperties.Provider p = properties.getProvider();
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
                messages.add(Map.of("role", "system", "content", request.systemPrompt()));
            }
            for (ChatMessage msg : request.messages()) {
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("model", p.getModelName() != null && !p.getModelName().contains("待核实") ? p.getModelName() : "default");
            reqBody.put("messages", messages);
            reqBody.put("temperature", 0.3);

            String jsonPayload = objectMapper.writeValueAsString(reqBody);

            HttpRequest.Builder httpReqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(p.getEndpointUrl()))
                    .timeout(Duration.ofMillis(p.getSyncTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

            if (p.getApiKey() != null && !p.getApiKey().isBlank()) {
                httpReqBuilder.header("Authorization", "Bearer " + p.getApiKey());
            }

            HttpResponse<String> resp = httpClient.send(httpReqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int statusCode = resp.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                log.warn("大模型服务响应非 2xx: status={}, body={}", statusCode, resp.body());
                throw new ProviderException("AI_HTTP_ERROR", "大模型接口返回异常状态码: " + statusCode);
            }

            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String reply = choices.get(0).path("message").path("content").asText();
                int elapsed = (int) (System.currentTimeMillis() - start);
                return new ChatResponse(reply, p.getModelName(), elapsed);
            }
            throw new ProviderException("AI_EMPTY_RESPONSE", "大模型未返回有效内容");
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用大模型服务发生未知异常", e);
            throw new ProviderException("AI_CALL_FAILED", "调用大模型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String promptVersion() {
        return properties.getProvider().getPromptVersion();
    }
}
