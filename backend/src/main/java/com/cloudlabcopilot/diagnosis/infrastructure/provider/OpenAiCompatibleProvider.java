package com.cloudlabcopilot.diagnosis.infrastructure.provider;

import com.cloudlabcopilot.diagnosis.domain.port.ChatProviderPort;
import com.cloudlabcopilot.diagnosis.infrastructure.config.DiagnosisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudlab.diagnosis.provider.type", havingValue = "OPENAI_COMPATIBLE")
public class OpenAiCompatibleProvider implements ChatProviderPort {

    private final DiagnosisProperties properties;

    public OpenAiCompatibleProvider(DiagnosisProperties properties) {
        this.properties = properties;
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
                && p.getApiKey() != null
                && !p.getApiKey().isBlank();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (!isConfigured()) {
            throw new ProviderException("AI_UNCONFIGURED", "OpenAI 兼容 Provider 未配置");
        }
        throw new ProviderException("AI_UNAVAILABLE", "OpenAI 兼容端点待配置，暂不可用");
    }

    @Override
    public String promptVersion() {
        return properties.getProvider().getPromptVersion();
    }
}