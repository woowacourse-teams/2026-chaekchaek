import XCTest
@testable import Chaekchaek

final class ChaekchaekTests: XCTestCase {
    func testAladinResponseMapsFlexiblePageAndCategory() throws {
        let data = Data(#"{"item":[{"title":"테스트 책","author":"참새","publisher":"첵췍","isbn13":"123","categoryName":"국내도서>소설","itemPage":"321"}]}"#.utf8)

        let book = try XCTUnwrap(AladinBookSearchClient.decode(data).first)

        XCTAssertEqual(book.id, "123")
        XCTAssertEqual(book.category, "소설")
        XCTAssertEqual(book.totalPages, 321)
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
}
