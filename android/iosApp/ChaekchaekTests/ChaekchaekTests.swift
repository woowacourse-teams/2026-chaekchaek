import XCTest
@testable import Chaekchaek

final class ChaekchaekTests: XCTestCase {
    func testServerSearchResponseMapsBook() throws {
        let data = Data(#"{"items":[{"title":"테스트 책","coverImageUrl":"https://example.com/cover.jpg","authors":["참새"],"translators":[],"publishedDate":"2026-08-19","isbn13":"123","category":"국내도서>소설","publisher":"첵췍"}]}"#.utf8)

        let book = try XCTUnwrap(BookSearchClient.decode(data).first)

        XCTAssertEqual(book.id, "123")
        XCTAssertEqual(book.category, "소설")
        XCTAssertEqual(book.author, "참새")
    }

    @MainActor
    func testLibraryPersistsAfterStatusChange() throws {
        let suiteName = "ChaekchaekTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let book = Book(
            isbn13: "123",
            isbn: "",
            title: "테스트 책",
            author: "참새",
            publisher: "첵췍",
            coverURL: nil,
            category: "소설",
            publishedAt: "2026-08-19",
            totalPages: 100,
            description: ""
        )
        let model = AppModel(defaults: defaults)
        model.save(book, as: .wantToRead)
        model.changeStatus(of: try XCTUnwrap(model.library.first), to: .reading)

        let reloaded = AppModel(defaults: defaults)

        XCTAssertEqual(reloaded.library.first?.status, .reading)
    }

    @MainActor
    func testStatusChangeMovesBookToMostRecentPosition() throws {
        let suiteName = "ChaekchaekTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let first = Book(isbn13: "1", isbn: "", title: "첫 책", author: "참새", publisher: "첵췍", coverURL: nil, category: "소설", publishedAt: "", totalPages: 0, description: "")
        let second = Book(isbn13: "2", isbn: "", title: "둘째 책", author: "참새", publisher: "첵췍", coverURL: nil, category: "소설", publishedAt: "", totalPages: 0, description: "")
        let model = AppModel(defaults: defaults)
        model.save(first, as: .wantToRead)
        model.save(second, as: .wantToRead)

        model.changeStatus(of: try XCTUnwrap(model.library.last), to: .reading)

        XCTAssertEqual(model.library.first?.id, first.id)
    }
}
