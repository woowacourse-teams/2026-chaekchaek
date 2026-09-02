package com.chaekchaek.library.dto;

import com.chaekchaek.member.domain.Member;
import java.util.List;

public record PublicLibraryListResponse(
        PublicMemberResponse member,
        long totalCount,
        long filteredCount,
        Integer nextPage,
        List<PublicLibraryItemResponse> items
) {
    public static PublicLibraryListResponse from(Member member, LibraryListResponse library) {
        return new PublicLibraryListResponse(PublicMemberResponse.from(member), library.totalCount(),
                library.filteredCount(), library.nextPage(),
                library.items().stream().map(PublicLibraryItemResponse::from).toList());
    }
}
