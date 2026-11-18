package com.cloudlabcopilot.diagnosis.infrastructure.provider;

import com.cloudlabcopilot.diagnosis.domain.port.ChatProviderPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudlab.diagnosis.provider.type", havingValue = "NONE", matchIfMissing = true)
public class NoneProvider implements ChatProviderPort {

    @Override
    public String providerType() {
        return "NONE";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        throw new ProviderException("AI_UNCONFIGURED", "AI Provider 未配置，使用知识兜底");
    }

    @Override
    public String promptVersion() {
        return "diag-p1";
    }
}