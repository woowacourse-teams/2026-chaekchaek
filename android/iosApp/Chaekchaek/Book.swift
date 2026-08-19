import Foundation

enum ReadingStatus: String, Codable, CaseIterable, Identifiable, Sendable {
    case wantToRead
    case reading
    case finished

    var id: Self { self }

    var title: String {
        switch self {
        case .wantToRead: "읽고 싶어요"
        case .reading: "읽는 중"
        case .finished: "다 읽음"
        }
    }
}

struct Book: Codable, Hashable, Identifiable, Sendable {
    let isbn13: String
    let isbn: String
    let title: String
    let author: String
    let publisher: String
    let coverURL: URL?
    let category: String
    let publishedAt: String
    let totalPages: Int
    let description: String

    var id: String {
        if !isbn13.isEmpty { return isbn13 }
        if !isbn.isEmpty { return isbn }
        return "\(title)|\(author)|\(publisher)"
    }

    init(
        isbn13: String,
        isbn: String,
        title: String,
        author: String,
        publisher: String,
        coverURL: URL?,
        category: String,
        publishedAt: String,
        totalPages: Int,
        description: String
    ) {
        self.isbn13 = isbn13
        self.isbn = isbn
        self.title = title
        self.author = author
        self.publisher = publisher
        self.coverURL = coverURL
        self.category = category
        self.publishedAt = publishedAt
        self.totalPages = max(0, totalPages)
        self.description = description
    }
}

struct LibraryBook: Codable, Hashable, Identifiable, Sendable {
    let book: Book
    var status: ReadingStatus
    var recordedAt: Date

    var id: String { book.id }
}

struct BookSearchClient: Sendable {
    enum SearchError: LocalizedError, Sendable {
        case invalidResponse

        var errorDescription: String? {
            switch self {
            case .invalidResponse:
                "검색 결과를 읽지 못했어요."
            }
        }
    }

    func search(_ query: String) async throws -> [Book] {
        var components = URLComponents(string: "https://api.chaekchaek.com/api/v1/books")
        components?.queryItems = [
            URLQueryItem(name: "query", value: query),
            URLQueryItem(name: "page", value: "1")
        ]
        guard let url = components?.url else { throw SearchError.invalidResponse }

        var request = URLRequest(url: url)
        request.timeoutInterval = 10
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, 200..<300 ~= httpResponse.statusCode else {
            throw SearchError.invalidResponse
        }
        return try Self.decode(data)
    }

    static func decode(_ data: Data) throws -> [Book] {
        let response = try JSONDecoder().decode(Response.self, from: data)
        return response.items.map(\.book)
    }

    private struct Response: Decodable {
        let items: [Item]
    }

    private struct Item: Decodable {
        let title: String
        let coverImageUrl: String
        let authors: [String]
        let translators: [String]
        let publishedDate: String
        let isbn13: String
        let category: String
        let publisher: String

        var book: Book {
            Book(
                isbn13: isbn13,
                isbn: "",
                title: title,
                author: (authors + translators.map { "\($0) 옮김" }).joined(separator: " · "),
                publisher: publisher,
                coverURL: URL(string: coverImageUrl),
                category: category.split(separator: ">").last.map(String.init) ?? "",
                publishedAt: publishedDate,
                totalPages: 0,
                description: ""
            )
        }
    }
}
