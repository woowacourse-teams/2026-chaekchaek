package com.chaekchaek.library.service;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.DisplayOrder;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryOrderService {

    private final MemberRepository memberRepository;
    private final LibraryRepository libraryRepository;
    private final LibraryBookRepository libraryBookRepository;

    @Transactional
    public void reorderLibraries(long memberId, List<Long> libraryIds) {
        lockActiveMember(memberId);
        DisplayOrder order = new DisplayOrder(libraryIds);
        List<Library> libraries = libraryRepository.findAllByMemberIdOrderByDisplayOrderAscIdAsc(memberId);
        Map<Long, Library> librariesById = libraries.stream()
                .collect(Collectors.toMap(Library::getId, Function.identity()));
        order.requireSameIds(List.copyOf(librariesById.keySet()));
        long position = 1L;
        for (long libraryId : order.ids()) {
            librariesById.get(libraryId).changeDisplayOrder(position++);
        }
    }

    @Transactional
    public void reorderBooks(long memberId, long libraryId, List<Long> bookIds) {
        lockActiveMember(memberId);
        requireOwnedLibrary(memberId, libraryId);
        DisplayOrder order = new DisplayOrder(bookIds);
        List<LibraryBook> placements = libraryBookRepository.findAllByLibraryIdOrderByDisplayOrderAscIdAsc(libraryId);
        Map<Long, LibraryBook> placementsByBookId = placements.stream()
                .collect(Collectors.toMap(LibraryBook::getBookId, Function.identity()));
        order.requireSameIds(List.copyOf(placementsByBookId.keySet()));
        long position = 1L;
        for (long bookId : order.ids()) {
            placementsByBookId.get(bookId).changeDisplayOrder(position++);
        }
    }

    private void requireOwnedLibrary(long memberId, long libraryId) {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));
        if (!library.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void lockActiveMember(long memberId) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
