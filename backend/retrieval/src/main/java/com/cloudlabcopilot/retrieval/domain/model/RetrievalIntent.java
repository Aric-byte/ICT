package com.cloudlabcopilot.retrieval.domain.model;

import java.util.List;

public record RetrievalIntent(
        String techStack,
        List<String> errorCodes,
        List<String> objectNames,
        String keywords) {
}