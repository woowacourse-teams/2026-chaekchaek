package com.chaekchaek.review.dto;

import java.util.List;

public record PageResponse<T>(long totalCount, Integer nextPage, List<T> items) {
}
