package com.cloudlabcopilot.diagnosis.infrastructure.provider;

import com.cloudlabcopilot.diagnosis.domain.port.ChatProviderPort;
import com.cloudlabcopilot.diagnosis.infrastructure.config.DiagnosisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudlab.diagnosis.provider.type", havingValue = "MAAS")
public class MaasChatProvider implements ChatProviderPort {

    private final DiagnosisProperties properties;

    public MaasChatProvider(DiagnosisProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerType() {
        return "MAAS";
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
            throw new ProviderException("AI_UNCONFIGURED", "MaaS 未配置（端点/模型/鉴权待核实）");
        }
        throw new ProviderException("AI_UNAVAILABLE", "MaaS 端点与鉴权待核实，暂不可用");
    }

    @Override
    public String promptVersion() {
        return properties.getProvider().getPromptVersion();
    }
}