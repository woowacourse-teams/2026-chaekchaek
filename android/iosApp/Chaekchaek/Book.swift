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

struct AladinBookSearchClient: Sendable {
    enum SearchError: LocalizedError, Sendable {
        case missingAPIKey
        case invalidResponse
        case server(String)

        var errorDescription: String? {
            switch self {
            case .missingAPIKey:
                "알라딘 API 키가 설정되지 않았어요."
            case .invalidResponse:
                "검색 결과를 읽지 못했어요."
            case let .server(message):
                message
            }
        }
    }

    func search(_ query: String) async throws -> [Book] {
        guard
            let apiKey = Bundle.main.object(forInfoDictionaryKey: "ALADIN_TTB_KEY") as? String,
            !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else {
            throw SearchError.missingAPIKey
        }

        var components = URLComponents(string: "https://www.aladin.co.kr/ttb/api/ItemSearch.aspx")
        components?.queryItems = [
            URLQueryItem(name: "ttbkey", value: apiKey),
            URLQueryItem(name: "Query", value: query),
            URLQueryItem(name: "QueryType", value: "Keyword"),
            URLQueryItem(name: "MaxResults", value: "20"),
            URLQueryItem(name: "start", value: "1"),
            URLQueryItem(name: "SearchTarget", value: "Book"),
            URLQueryItem(name: "output", value: "js"),
            URLQueryItem(name: "Version", value: "20131101"),
            URLQueryItem(name: "Cover", value: "Big")
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
        if let message = response.errorMessage, !message.isEmpty {
            throw SearchError.server(message)
        }
        return response.item?.map(\.book) ?? []
    }

    private struct Response: Decodable {
        let item: [Item]?
        let errorMessage: String?
    }

    private struct Item: Decodable {
        let title: String?
        let author: String?
        let publisher: String?
        let cover: String?
        let isbn: String?
        let isbn13: String?
        let categoryName: String?
        let pubDate: String?
        let itemPage: Int?
        let description: String?

        enum CodingKeys: String, CodingKey {
            case title, author, publisher, cover, isbn, isbn13, categoryName, pubDate, itemPage, description
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            title = try container.decodeIfPresent(String.self, forKey: .title)
            author = try container.decodeIfPresent(String.self, forKey: .author)
            publisher = try container.decodeIfPresent(String.self, forKey: .publisher)
            cover = try container.decodeIfPresent(String.self, forKey: .cover)
            isbn = try container.decodeIfPresent(String.self, forKey: .isbn)
            isbn13 = try container.decodeIfPresent(String.self, forKey: .isbn13)
            categoryName = try container.decodeIfPresent(String.self, forKey: .categoryName)
            pubDate = try container.decodeIfPresent(String.self, forKey: .pubDate)
            description = try container.decodeIfPresent(String.self, forKey: .description)
            if let value = try? container.decode(Int.self, forKey: .itemPage) {
                itemPage = value
            } else if let value = try? container.decode(String.self, forKey: .itemPage) {
                itemPage = Int(value)
            } else {
                itemPage = nil
            }
        }

        var book: Book {
            Book(
                isbn13: isbn13 ?? "",
                isbn: isbn ?? "",
                title: title ?? "제목 없음",
                author: author ?? "저자 미상",
                publisher: publisher ?? "출판사 미상",
                coverURL: cover.flatMap(URL.init(string:)),
                category: categoryName?.split(separator: ">").last.map(String.init) ?? "",
                publishedAt: pubDate ?? "",
                totalPages: itemPage ?? 0,
                description: description ?? ""
            )
        }
    }
}
