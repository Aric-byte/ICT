package com.cloudlabcopilot.diagnosis.api.dto;

import java.util.List;

public record PageView<T>(List<T> items, int page, int pageSize, long total) {
}