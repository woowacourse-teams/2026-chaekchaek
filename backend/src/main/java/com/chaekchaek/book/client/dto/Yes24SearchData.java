package com.chaekchaek.book.client.dto;

import java.util.List;

public record Yes24SearchData(
        List<Yes24BookItem> items,
        int currentPage,
        int pageSize,
        int totalCount
) {

    public Yes24SearchData {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public Integer nextPage() {
        return (long) currentPage * pageSize < totalCount ? currentPage + 1 : null;
    }
}
