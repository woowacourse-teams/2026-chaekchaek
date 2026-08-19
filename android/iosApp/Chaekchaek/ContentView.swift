import SwiftUI

struct ContentView: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var model = model
        TabView(selection: $model.selectedTab) {
            NavigationStack { SearchView() }
                .tabItem { Label("검색", systemImage: "magnifyingglass") }
                .tag(0)
            NavigationStack { LibraryView() }
                .tabItem { Label("서재", systemImage: "books.vertical") }
                .tag(1)
            NavigationStack { InformationView() }
                .tabItem { Label("정보", systemImage: "info.circle") }
                .tag(2)
        }
        .tint(AppTheme.accent)
    }
}

private struct SearchView: View {
    @Environment(AppModel.self) private var model
    @FocusState private var searchFocused: Bool
    @State private var selectedBook: Book?

    var body: some View {
        @Bindable var model = model
        VStack(spacing: 0) {
            searchBar(query: $model.query)
                .padding(.horizontal, 20)
                .padding(.vertical, 14)
            searchContent
        }
        .background(AppTheme.background)
        .navigationTitle("책 검색")
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog(
            "서재에 어떤 상태로 담을까요?",
            isPresented: Binding(
                get: { selectedBook != nil },
                set: { if !$0 { selectedBook = nil } }
            ),
            titleVisibility: .visible
        ) {
            ForEach(ReadingStatus.allCases) { status in
                Button(status.title) {
                    guard let book = selectedBook else { return }
                    model.save(book, as: status)
                    selectedBook = nil
                }
            }
            Button("취소", role: .cancel) { selectedBook = nil }
        }
    }

    private func searchBar(query: Binding<String>) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(AppTheme.secondary)
            TextField("책 제목이나 저자를 검색해 보세요", text: query)
                .focused($searchFocused)
                .submitLabel(.search)
                .onSubmit { Task { await model.search() } }
                .accessibilityLabel("책 검색어")
            if !query.wrappedValue.isEmpty {
                Button {
                    query.wrappedValue = ""
                    model.searchState = .idle
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(AppTheme.secondary)
                }
                .accessibilityLabel("검색어 지우기")
            }
            Button("검색") {
                searchFocused = false
                Task { await model.search() }
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(AppTheme.accent)
            .disabled(query.wrappedValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
        .padding(.horizontal, 14)
        .frame(minHeight: 48)
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay {
            RoundedRectangle(cornerRadius: 12)
                .stroke(AppTheme.border, lineWidth: 1)
        }
    }

    @ViewBuilder
    private var searchContent: some View {
        switch model.searchState {
        case .idle:
            emptyMessage(icon: "book.closed", title: "어떤 책을 찾고 있나요?", description: "제목이나 저자를 검색해 서재에 담아 보세요.")
        case .loading:
            Spacer()
            ProgressView("책을 찾고 있어요")
            Spacer()
        case let .results(books):
            List(books) { book in
                searchResultRow(book)
                    .listRowBackground(AppTheme.background)
                    .listRowSeparatorTint(AppTheme.band)
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
        case .empty:
            emptyMessage(icon: "text.magnifyingglass", title: "검색 결과가 없어요", description: "다른 제목이나 저자로 다시 검색해 보세요.")
        case let .failure(message):
            VStack(spacing: 14) {
                emptyMessage(icon: "wifi.exclamationmark", title: "검색하지 못했어요", description: message)
                Button("다시 시도") { Task { await model.search() } }
                    .buttonStyle(.borderedProminent)
                    .tint(AppTheme.accent)
            }
        }
    }

    private func searchResultRow(_ book: Book) -> some View {
        HStack(alignment: .top, spacing: 14) {
            BookCover(book: book)
            VStack(alignment: .leading, spacing: 6) {
                Text(book.title)
                    .font(.headline)
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(2)
                Text([book.author, book.publisher].filter { !$0.isEmpty }.joined(separator: " · "))
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.secondary)
                    .lineLimit(2)
                if let status = model.status(for: book) {
                    Text(status.title)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(AppTheme.accent)
                }
            }
            Spacer(minLength: 4)
            Button {
                selectedBook = book
            } label: {
                Image(systemName: model.status(for: book) == nil ? "plus" : "checkmark")
                    .font(.headline)
                    .frame(width: 40, height: 40)
                    .foregroundStyle(model.status(for: book) == nil ? .white : AppTheme.accent)
                    .background(model.status(for: book) == nil ? AppTheme.accent : AppTheme.accentSoft)
                    .clipShape(Circle())
            }
            .accessibilityLabel(model.status(for: book) == nil ? "\(book.title) 서재에 담기" : "\(book.title) 독서 상태 변경")
        }
        .padding(.vertical, 8)
    }
}

private struct LibraryView: View {
    @Environment(AppModel.self) private var model
    @State private var filter: ReadingStatus?
    @State private var pendingDeletion: LibraryBook?

    private var filteredBooks: [LibraryBook] {
        guard let filter else { return model.library }
        return model.library.filter { $0.status == filter }
    }

    var body: some View {
        VStack(spacing: 0) {
            filters
                .padding(.vertical, 12)
            if filteredBooks.isEmpty {
                emptyLibrary
            } else {
                List(filteredBooks) { libraryBook in
                    libraryRow(libraryBook)
                        .listRowBackground(AppTheme.background)
                        .listRowSeparatorTint(AppTheme.band)
                        .swipeActions {
                            Button("삭제", role: .destructive) {
                                pendingDeletion = libraryBook
                            }
                        }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
        }
        .background(AppTheme.background)
        .navigationTitle("내 서재")
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog(
            "서재에서 삭제할까요?",
            isPresented: Binding(
                get: { pendingDeletion != nil },
                set: { if !$0 { pendingDeletion = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("삭제", role: .destructive) {
                guard let book = pendingDeletion else { return }
                model.remove(book)
                pendingDeletion = nil
            }
            Button("취소", role: .cancel) { pendingDeletion = nil }
        }
    }

    private var filters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                filterButton(title: "전체", status: nil)
                ForEach(ReadingStatus.allCases) { status in
                    filterButton(title: status.title, status: status)
                }
            }
            .padding(.horizontal, 20)
        }
    }

    private func filterButton(title: String, status: ReadingStatus?) -> some View {
        let selected = filter == status
        return Button(title) { filter = status }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(selected ? .white : AppTheme.secondary)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(selected ? AppTheme.accent : AppTheme.muted)
            .clipShape(Capsule())
            .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private var emptyLibrary: some View {
        VStack(spacing: 14) {
            Spacer()
            Image(systemName: "books.vertical")
                .font(.system(size: 36))
                .foregroundStyle(AppTheme.accent)
            Text(filter == nil ? "아직 담은 책이 없어요" : "이 상태의 책이 없어요")
                .font(.headline)
            Text("검색에서 책을 찾아 서재에 담아 보세요.")
                .font(.subheadline)
                .foregroundStyle(AppTheme.secondary)
            Button("책 검색하기") { model.selectedTab = 0 }
                .buttonStyle(.borderedProminent)
                .tint(AppTheme.accent)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private func libraryRow(_ libraryBook: LibraryBook) -> some View {
        HStack(alignment: .top, spacing: 14) {
            BookCover(book: libraryBook.book)
            VStack(alignment: .leading, spacing: 6) {
                Text(libraryBook.book.title)
                    .font(.headline)
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(2)
                Text(libraryBook.book.author)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.secondary)
                    .lineLimit(1)
                Menu {
                    ForEach(ReadingStatus.allCases) { status in
                        Button(status.title) {
                            model.changeStatus(of: libraryBook, to: status)
                        }
                    }
                } label: {
                    Label(libraryBook.status.title, systemImage: "chevron.down")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(AppTheme.accent)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(AppTheme.accentSoft)
                        .clipShape(Capsule())
                }
                .accessibilityLabel("\(libraryBook.book.title) 독서 상태, 현재 \(libraryBook.status.title)")
            }
            Spacer()
        }
        .padding(.vertical, 8)
    }
}

private struct InformationView: View {
    private let privacyURL = URL(string: "https://app.notion.com/p/3b185850b3e18085b919d108ce7cd4ef?source=copy_link")!
    private let supportURL = URL(string: "https://github.com/woowacourse-teams/2026-chaekchaek/issues")!

    var body: some View {
        List {
            Section("앱 정보") {
                LabeledContent("버전", value: version)
                Text("검색어는 도서 검색을 위해 알라딘 Open API로 전송됩니다. 서재 기록은 이 기기에만 저장됩니다.")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.secondary)
            }
            Section("정책 및 지원") {
                Link(destination: privacyURL) {
                    Label("개인정보처리방침", systemImage: "hand.raised")
                }
                Link(destination: supportURL) {
                    Label("문의 및 지원", systemImage: "questionmark.circle")
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(AppTheme.background)
        .tint(AppTheme.accent)
        .navigationTitle("정보")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var version: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "1"
        return "\(version) (\(build))"
    }
}

private func emptyMessage(icon: String, title: String, description: String) -> some View {
    VStack(spacing: 12) {
        Spacer()
        Image(systemName: icon)
            .font(.system(size: 36))
            .foregroundStyle(AppTheme.accent)
        Text(title)
            .font(.headline)
            .foregroundStyle(AppTheme.ink)
        Text(description)
            .font(.subheadline)
            .foregroundStyle(AppTheme.secondary)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 32)
        Spacer()
    }
    .frame(maxWidth: .infinity)
}
