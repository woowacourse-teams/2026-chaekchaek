package com.chaekchaek.app.domain.profile

import com.chaekchaek.app.data.remote.LatestReviewAuthorDto
import com.chaekchaek.app.data.remote.ReviewAuthorDto
import kotlinx.serialization.json.Json
import kotlin.test.*

class AuthorProfileTest {
    @Test fun onlyExplicitlyAvailableMemberCanOpenLibrary() {
        val member = AuthorProfile("독자", 42, actorType = "MEMBER", profileStatus = "AVAILABLE")
        assertEquals(LibraryAccess.Available, member.libraryAccess)
        assertEquals(LibraryAccess.Anonymous, member.copy(anonymous = true).libraryAccess)
        assertEquals(LibraryAccess.Guest, member.copy(actorType = "GUEST").libraryAccess)
        assertEquals(LibraryAccess.Withdrawn, member.copy(memberId = null, profileStatus = "UNAVAILABLE").libraryAccess)
        assertEquals(LibraryAccess.Unavailable, member.copy(memberId = null).libraryAccess)
        assertEquals(LibraryAccess.Unavailable, member.copy(memberId = 0).libraryAccess)
        assertEquals(LibraryAccess.Unavailable, member.copy(profileStatus = null).libraryAccess)
    }
    @Test fun anonymousAndWithdrawnProfilesDoNotExposeImages() {
        val profile = AuthorProfile("익명 독자", 42, "private-image", true, "MEMBER", "AVAILABLE")
        assertNull(profile.visibleImageUrl)
        assertNull(profile.copy(anonymous = false, profileStatus = "UNAVAILABLE").visibleImageUrl)
    }
    @Test fun homeAndReviewAuthorContractsKeepTheSameIdentityAndAccess() {
        val json = """{"memberId":42,"displayName":"독자","profileImageUrl":null,"anonymous":false,"mine":false,"actorType":"MEMBER","profileStatus":"AVAILABLE"}"""
        val parser = Json { ignoreUnknownKeys = true }
        val latest = parser.decodeFromString<LatestReviewAuthorDto>(json).toAuthorProfile()
        val review = parser.decodeFromString<ReviewAuthorDto>(json).toAuthorProfile()
        assertEquals(latest, review)
        assertEquals(42L, latest.memberId)
        assertEquals(LibraryAccess.Available, latest.libraryAccess)
    }
    @Test fun legacyAuthorWithNoIdentityFailsClosed() {
        val author = Json.decodeFromString<LatestReviewAuthorDto>("""{"displayName":"독자"}""").toAuthorProfile()
        assertEquals(LibraryAccess.Unavailable, author.libraryAccess)
    }
}
