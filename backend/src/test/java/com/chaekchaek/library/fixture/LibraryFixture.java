package com.chaekchaek.library.fixture;

import com.chaekchaek.library.domain.Library;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class LibraryFixture {

    private LibraryFixture() {
    }

    public static Library savedLibrary(long id, long memberId, Instant createdAt) {
        return savedLibrary(id, Library.create(memberId, "가을책장", createdAt));
    }

    public static Library savedLibrary(long id, Library library) {
        ReflectionTestUtils.setField(library, "id", id);
        return library;
    }
}
