package com.edifiqapi.web;

import java.util.List;
import java.util.Map;

public record ApiResponse<T>(T data, Meta meta, Map<String, Object> links) {

    public record Meta(int total) {}

    /** Single item or mutation result — no pagination metadata. */
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null, Map.of());
    }

    /** Collection result with total count. */
    public static <T> ApiResponse<List<T>> of(List<T> data) {
        return new ApiResponse<>(data, new Meta(data.size()), Map.of());
    }
}
