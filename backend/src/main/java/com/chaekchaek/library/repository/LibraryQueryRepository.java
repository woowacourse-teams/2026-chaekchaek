package com.chaekchaek.library.repository;

import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryBookDetailResponse;
import com.chaekchaek.library.dto.LibraryBookSummaryResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LibraryQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<LibraryHeader> findLibrariesByMemberId(long memberId) {
        return jdbcTemplate.query(
                """
                select id, member_id, name, is_default
                from library
                where member_id = ?
                order by display_order, id
                """,
                (rs, row) -> libraryHeader(rs),
                memberId
        );
    }

    public List<PlacedBook> findAllBooksByMemberId(long memberId) {
        List<PlacedBook> placedBooks = jdbcTemplate.query(
                """
                select lb.library_id, lb.book_id, b.title, b.cover_image_url, b.spine_image_url, b.isbn13,
                       li.status, li.current_page, li.reading_updated_at
                from library l
                join library_book lb on lb.library_id = l.id
                left join book b on b.id = lb.book_id
                left join library_item li on li.member_id = l.member_id and li.book_id = lb.book_id
                where l.member_id = ?
                order by l.display_order, l.id, lb.display_order, lb.id
                """,
                (rs, row) -> {
                    assertReadingState(rs);
                    return new PlacedBook(
                            rs.getLong("library_id"),
                            new LibraryBookSummaryResponse(
                                    rs.getLong("book_id"),
                                    rs.getString("title"),
                                    rs.getString("cover_image_url"),
                                    rs.getString("spine_image_url"),
                                    rs.getString("isbn13"),
                                    List.of()
                            )
                    );
                },
                memberId
        );
        if (placedBooks.isEmpty()) {
            return placedBooks;
        }
        List<Long> bookIds = placedBooks
                .stream()
                .map(placedBook -> placedBook.book().bookId())
                .distinct()
                .toList();
        Map<Long, List<String>> authorsByBookId = authorsByBookId(bookIds);
        return placedBooks
                .stream()
                .map(placedBook -> new PlacedBook(
                        placedBook.libraryId(),
                        placedBook.book().withAuthors(
                                authorsByBookId.getOrDefault(placedBook.book().bookId(), List.of())
                        )
                ))
                .toList();
    }

    public Optional<LibraryHeader> findPublicLibraryById(long libraryId) {
        return jdbcTemplate.query(
                """
                select l.id, l.member_id, l.name, l.is_default
                from library l
                join member m on m.member_id = l.member_id
                where l.id = ? and m.account_status = 'ACTIVE'
                """,
                (rs, row) -> libraryHeader(rs),
                libraryId
        )
                .stream()
                .findFirst();
    }

    public List<LibraryBookDetailResponse> findBooksByLibraryId(long libraryId) {
        List<LibraryBookDetailResponse> books = jdbcTemplate.query(
                """
                select lb.book_id, b.title, b.cover_image_url, b.spine_image_url, b.isbn13, b.total_pages,
                       li.status, li.current_page, li.reading_updated_at
                from library_book lb
                join library l on l.id = lb.library_id
                left join book b on b.id = lb.book_id
                left join library_item li on li.member_id = l.member_id and li.book_id = lb.book_id
                where lb.library_id = ?
                order by lb.display_order, lb.id
                """,
                (rs, row) -> {
                    assertReadingState(rs);
                    return new LibraryBookDetailResponse(
                            rs.getLong("book_id"),
                            rs.getString("title"),
                            rs.getString("cover_image_url"),
                            rs.getString("spine_image_url"),
                            rs.getString("isbn13"),
                            List.of(),
                            rs.getObject("total_pages", Integer.class),
                            ReadingStatus.valueOf(rs.getString("status")),
                            rs.getInt("current_page"),
                            rs.getTimestamp("reading_updated_at", Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant()
                    );
                },
                libraryId
        );
        if (books.isEmpty()) {
            return books;
        }
        List<Long> bookIds = books
                .stream()
                .map(LibraryBookDetailResponse::bookId)
                .toList();
        Map<Long, List<String>> authorsByBookId = authorsByBookId(bookIds);
        return books
                .stream()
                .map(book -> book.withAuthors(authorsByBookId.getOrDefault(book.bookId(), List.of())))
                .toList();
    }

    private Map<Long, List<String>> authorsByBookId(List<Long> bookIds) {
        Map<Long, List<String>> authorsByBookId = new HashMap<>();
        String placeholders = String.join(", ", Collections.nCopies(bookIds.size(), "?"));
        jdbcTemplate.query(
                "select book_id, name from book_author where book_id in (" + placeholders + ") "
                        + "order by book_id, display_order",
                rs -> {
                    authorsByBookId.computeIfAbsent(rs.getLong("book_id"), ignored -> new ArrayList<>())
                            .add(rs.getString("name"));
                },
                bookIds.toArray()
        );
        return authorsByBookId;
    }

    private LibraryHeader libraryHeader(ResultSet rs) throws SQLException {
        return new LibraryHeader(
                rs.getLong("id"),
                rs.getLong("member_id"),
                rs.getString("name"),
                rs.getBoolean("is_default")
        );
    }

    private void assertReadingState(ResultSet rs) throws SQLException {
        if (rs.getString("isbn13") == null || rs.getString("status") == null
                || rs.getObject("current_page") == null || rs.getObject("reading_updated_at") == null) {
            throw new IllegalStateException("Placed books must have book metadata and the owner's reading state");
        }
    }

    public record LibraryHeader(
            long libraryId,
            long memberId,
            String name,
            boolean isDefault
    ) {
    }

    public record PlacedBook(
            long libraryId,
            LibraryBookSummaryResponse book
    ) {
    }
}
