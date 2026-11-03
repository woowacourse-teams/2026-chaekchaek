package com.chaekchaek.library.domain;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DisplayOrder {

    private final List<Long> ids;

    public DisplayOrder(List<Long> ids) {
        if (ids == null || ids.stream()
                .anyMatch(id -> id == null || id < 1)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.ids = List.copyOf(ids);
    }

    public void requireSameIds(List<Long> currentIds) {
        if (!Set.copyOf(ids).equals(Set.copyOf(currentIds))) {
            throw new BusinessException(ErrorCode.ORDER_CONFLICT);
        }
    }

    public List<Long> ids() {
        return ids;
    }
}
