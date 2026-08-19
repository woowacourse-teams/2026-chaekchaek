import Foundation
import Observation

@MainActor
@Observable
final class AppModel {
    enum SearchState: Equatable {
        case idle
        case loading
        case results([Book])
        case empty
        case failure(String)
    }

    private static let storageKey = "library.books"
    private let defaults: UserDefaults
    private let searchClient: BookSearchClient

    var selectedTab = 0
    var query = ""
    var searchState: SearchState = .idle
    private(set) var library: [LibraryBook]

    init(
        defaults: UserDefaults = .standard,
        searchClient: BookSearchClient = BookSearchClient()
    ) {
        self.defaults = defaults
        self.searchClient = searchClient
        library = Self.load(from: defaults)
    }

    func search() async {
        let trimmedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedQuery.isEmpty else {
            searchState = .idle
            return
        }

        searchState = .loading
        do {
            let books = try await searchClient.search(trimmedQuery)
            searchState = books.isEmpty ? .empty : .results(books)
        } catch {
            searchState = .failure(error.localizedDescription)
        }
    }

    func status(for book: Book) -> ReadingStatus? {
        library.first(where: { $0.id == book.id })?.status
    }

    func save(_ book: Book, as status: ReadingStatus) {
        if let index = library.firstIndex(where: { $0.id == book.id }) {
            library[index].status = status
            library[index].recordedAt = Date()
        } else {
            library.insert(LibraryBook(book: book, status: status, recordedAt: Date()), at: 0)
        }
        persist()
    }

    func changeStatus(of libraryBook: LibraryBook, to status: ReadingStatus) {
        guard let index = library.firstIndex(where: { $0.id == libraryBook.id }) else { return }
        library[index].status = status
        library[index].recordedAt = Date()
        library.insert(library.remove(at: index), at: 0)
        persist()
    }

    func remove(_ libraryBook: LibraryBook) {
        library.removeAll(where: { $0.id == libraryBook.id })
        persist()
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(library) else { return }
        defaults.set(data, forKey: Self.storageKey)
    }

    private static func load(from defaults: UserDefaults) -> [LibraryBook] {
        guard
            let data = defaults.data(forKey: storageKey),
            let books = try? JSONDecoder().decode([LibraryBook].self, from: data)
        else {
            return []
        }
        return books
    }
}
