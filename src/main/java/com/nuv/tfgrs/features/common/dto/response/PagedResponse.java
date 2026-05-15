package com.nuv.tfgrs.features.common.dto.response;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        long total,
        int limit,
        int offset
) {
}
