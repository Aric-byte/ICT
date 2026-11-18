package com.cloudlabcopilot.diagnosis.domain.port;

import java.util.List;

public interface ChatProviderPort {

    String providerType();

    boolean isConfigured();

    ChatResponse chat(ChatRequest request);

    String promptVersion();

    record ChatRequest(String systemPrompt, List<ChatMessage> messages, int maxOutputChars) {
    }

    record ChatMessage(String role, String content) {
    }

    record ChatResponse(String content, String modelId, int elapsedMs) {
    }

    class ProviderException extends RuntimeException {

        private final String category;

        public ProviderException(String category, String message) {
            super(message);
            this.category = category;
        }

        public ProviderException(String category, String message, Throwable cause) {
            super(message, cause);
            this.category = category;
        }

        public String getCategory() {
            return category;
        }
    }
}