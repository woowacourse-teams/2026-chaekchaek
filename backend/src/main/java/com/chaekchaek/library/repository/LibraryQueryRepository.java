package com.chaekchaek.library.repository;

import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryBookDetailResponse;
import com.chaekchaek.library.dto.LibraryBookSummaryResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
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
        return jdbcTemplate.query(
                """
                select lb.library_id, lb.book_id, b.title, b.spine_image_url, b.isbn13,
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
                                    rs.getString("spine_image_url"),
                                    rs.getString("isbn13")
                            )
                    );
                },
                memberId
        );
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

    public long countBooksByLibraryId(long libraryId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from library_book where library_id = ?",
                Long.class,
                libraryId
        );
    }

    public List<LibraryBookDetailResponse> findBooksByLibraryId(long libraryId, long offset, int limit) {
        return jdbcTemplate.query(
                """
                select lb.book_id, b.title, b.spine_image_url, b.isbn13, b.total_pages,
                       li.status, li.current_page, li.reading_updated_at
                from library_book lb
                join library l on l.id = lb.library_id
                left join book b on b.id = lb.book_id
                left join library_item li on li.member_id = l.member_id and li.book_id = lb.book_id
                where lb.library_id = ?
                order by lb.display_order, lb.id
                limit ? offset ?
                """,
                (rs, row) -> {
                    assertReadingState(rs);
                    return new LibraryBookDetailResponse(
                            rs.getLong("book_id"),
                            rs.getString("title"),
                            rs.getString("spine_image_url"),
                            rs.getString("isbn13"),
                            rs.getObject("total_pages", Integer.class),
                            ReadingStatus.valueOf(rs.getString("status")),
                            rs.getInt("current_page"),
                            rs.getTimestamp("reading_updated_at", Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant()
                    );
                },
                libraryId,
                limit,
                offset
        );
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
