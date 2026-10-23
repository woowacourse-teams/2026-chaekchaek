package com.chaekchaek.library.service;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LibraryBookService {

    private final LibraryRepository libraryRepository;
    private final LibraryBookRepository libraryBookRepository;
    private final LibraryItemRepository libraryItemRepository;
    private final MemberRepository memberRepository;
    private final TransactionTemplate writeTransaction;

    public LibraryBookService(
            LibraryRepository libraryRepository,
            LibraryBookRepository libraryBookRepository,
            LibraryItemRepository libraryItemRepository,
            MemberRepository memberRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.libraryRepository = libraryRepository;
        this.libraryBookRepository = libraryBookRepository;
        this.libraryItemRepository = libraryItemRepository;
        this.memberRepository = memberRepository;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    public void deleteLibrary(long memberId, long libraryId) {
        writeTransaction.executeWithoutResult(transactionStatus -> {
            lockActiveMember(memberId);
            Library library = requireOwnedLibrary(memberId, libraryId);
            library.assertDeletable();
            deleteLibraryPlacements(memberId, libraryId);
            libraryRepository.delete(library);
        });
    }

    private void deleteLibraryPlacements(long memberId, long libraryId) {
        List<LibraryBook> placements = libraryBookRepository.findAllByLibraryId(libraryId);
        if (placements.isEmpty()) {
            return;
        }
        List<Long> bookIds = placements
                .stream()
                .map(LibraryBook::getBookId)
                .toList();
        libraryBookRepository.deleteAll(placements);
        libraryBookRepository.flush();
        clearLastReadingStates(memberId, bookIds);
    }

    private void clearLastReadingStates(long memberId, List<Long> bookIds) {
        Set<Long> placedBookIds = Set.copyOf(
                libraryBookRepository.findPlacedBookIdsByMemberIdAndBookIdIn(memberId, bookIds)
        );
        List<Long> lastBookIds = bookIds
                .stream()
                .filter(bookId -> !placedBookIds.contains(bookId))
                .toList();
        if (lastBookIds.isEmpty()) {
            return;
        }
        List<LibraryItem> items = libraryItemRepository.findAllByMemberIdAndBookIdInForUpdate(memberId, lastBookIds);
        if (items.size() != lastBookIds.size()) {
            throw new IllegalStateException("A placed book must have a shared library item");
        }
        items.forEach(LibraryItem::clearReadingState);
    }

    private void lockActiveMember(long memberId) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Library requireOwnedLibrary(long memberId, long libraryId) {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));
        if (!library.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return library;
    }
}
