package com.cloudlabcopilot.common.response;

import java.util.List;

public record PagedResult<T>(List<T> items, long total, int page, int size) {

    public static <T> PagedResult<T> of(List<T> items, long total, int page, int size) {
        return new PagedResult<>(items, total, page, size);
    }
}