package com.chaekchaek.library.service;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.dto.LibraryBookDetailResponse;
import com.chaekchaek.library.dto.LibraryBookSummaryResponse;
import com.chaekchaek.library.dto.LibraryCollectionResponse;
import com.chaekchaek.library.dto.LibraryDetailResponse;
import com.chaekchaek.library.dto.LibraryShelfResponse;
import com.chaekchaek.library.repository.LibraryQueryRepository;
import com.chaekchaek.library.repository.LibraryQueryRepository.LibraryHeader;
import com.chaekchaek.library.repository.LibraryQueryRepository.PlacedBook;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.repository.MemberRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class LibraryQueryService {

    private static final int PAGE_SIZE = 10;

    private final MemberRepository memberRepository;
    private final LibraryQueryRepository libraryQueryRepository;

    public LibraryCollectionResponse findLibraries(long memberId) {
        memberRepository.findById(memberId)
                .filter(member -> member.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        List<LibraryHeader> libraries = libraryQueryRepository.findLibrariesByMemberId(memberId);
        Map<Long, List<LibraryBookSummaryResponse>> booksByLibrary = libraryQueryRepository
                .findAllBooksByMemberId(memberId)
                .stream()
                .collect(Collectors.groupingBy(
                        PlacedBook::libraryId,
                        Collectors.mapping(PlacedBook::book, Collectors.toList())
                ));
        List<LibraryShelfResponse> shelves = libraries
                .stream()
                .map(library -> shelfResponse(library, booksByLibrary))
                .toList();
        return new LibraryCollectionResponse(
                memberId,
                shelves
        );
    }

    private LibraryShelfResponse shelfResponse(
            LibraryHeader library,
            Map<Long, List<LibraryBookSummaryResponse>> booksByLibrary
    ) {
        List<LibraryBookSummaryResponse> books = booksByLibrary.getOrDefault(library.libraryId(), List.of());
        return new LibraryShelfResponse(
                library.libraryId(),
                library.name(),
                library.isDefault(),
                books.size(),
                books
        );
    }

    public LibraryDetailResponse findLibrary(long libraryId, int page) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        LibraryHeader library = libraryQueryRepository.findPublicLibraryById(libraryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));
        long totalCount = libraryQueryRepository.countBooksByLibraryId(libraryId);
        long offset = ((long) page - 1) * PAGE_SIZE;
        List<LibraryBookDetailResponse> books = libraryQueryRepository.findBooksByLibraryId(
                libraryId,
                offset,
                PAGE_SIZE
        );
        Integer nextPage = null;
        if (offset + books.size() < totalCount && page < Integer.MAX_VALUE) {
            nextPage = page + 1;
        }
        return new LibraryDetailResponse(
                library.libraryId(),
                library.memberId(),
                library.name(),
                library.isDefault(),
                totalCount,
                nextPage,
                books
        );
    }
}
