import SwiftUI

struct ContentView: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var model = model
        TabView(selection: $model.selectedTab) {
            NavigationStack { HomeView() }
                .tabItem { Label("홈", systemImage: "house") }
                .tag(0)
            NavigationStack { SearchView() }
                .tabItem { Label("발견", systemImage: "magnifyingglass") }
                .tag(1)
            NavigationStack { LibraryView() }
                .tabItem { Label("내 서재", systemImage: "books.vertical") }
                .tag(2)
        }
        .tint(AppTheme.accent)
    }
}

private struct HomeView: View {
    @Environment(AppModel.self) private var model

    private var readingBook: LibraryBook? {
        model.library.first(where: { $0.status == .reading })
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            pageHeader("첵첵")
            if let readingBook {
                VStack(alignment: .leading, spacing: 12) {
                    Text("이어서 읽기")
                        .font(.headline)
                        .foregroundStyle(AppTheme.ink)
                    HStack(spacing: 12) {
                        BookCover(book: readingBook.book)
                        VStack(alignment: .leading, spacing: 5) {
                            Text(readingBook.book.title)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(AppTheme.ink)
                                .lineLimit(2)
                            Text(readingBook.book.author)
                                .font(.caption)
                                .foregroundStyle(AppTheme.secondary)
                            Text("내 서재에서 독서 기록을 남겨보세요")
                                .font(.caption)
                                .foregroundStyle(AppTheme.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(AppTheme.secondary)
                    }
                }
                .padding(16)
                .background(AppTheme.surface)
                .overlay { RoundedRectangle(cornerRadius: 4).stroke(AppTheme.border, lineWidth: 1) }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            } else {
                VStack(alignment: .leading, spacing: 12) {
                    Text("지금 읽고 있는 책이 있으세요?")
                        .font(.headline)
                        .foregroundStyle(AppTheme.ink)
                    Text("책을 등록하면 내 서재에서 독서 상태를 기록할 수 있어요.")
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.secondary)
                    Button {
                        model.selectedTab = 1
                    } label: {
                        Label("책 제목으로 찾기", systemImage: "magnifyingglass")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(AppTheme.ink)
                            .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                            .padding(.horizontal, 12)
                            .overlay { RoundedRectangle(cornerRadius: 4).stroke(AppTheme.ink, lineWidth: 1) }
                    }
                    .buttonStyle(.plain)
                }
                .padding(16)
                .background(AppTheme.surface)
                .overlay { RoundedRectangle(cornerRadius: 4).stroke(AppTheme.border, lineWidth: 1) }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            Spacer()
        }
        .background(AppTheme.background)
        .toolbar(.hidden, for: .navigationBar)
    }
}

private struct SearchView: View {
    @Environment(AppModel.self) private var model
    @FocusState private var searchFocused: Bool
    @State private var selectedBook: Book?

    var body: some View {
        @Bindable var model = model
        VStack(spacing: 0) {
            pageHeader("검색")
            searchBar(query: $model.query)
                .padding(.horizontal, 16)
                .padding(.bottom, 12)
            searchContent
        }
        .background(AppTheme.background)
        .toolbar(.hidden, for: .navigationBar)
        .sheet(item: $selectedBook) { book in
            StatusChangeSheet(initialStatus: model.status(for: book) ?? .wantToRead) { status in
                model.save(book, as: status)
                selectedBook = nil
            } onCancel: {
                selectedBook = nil
            }
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
            Button {
                searchFocused = false
                Task { await model.search() }
            } label: {
                Image(systemName: "arrow.right")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.ink)
                    .frame(width: 28, height: 28)
            }
            .disabled(query.wrappedValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
        .padding(.horizontal, 12)
        .frame(height: 44)
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 4))
        .overlay {
            RoundedRectangle(cornerRadius: 4)
                .stroke(AppTheme.border, lineWidth: 1)
        }
    }

    @ViewBuilder
    private var searchContent: some View {
        switch model.searchState {
        case .idle:
            emptyMessage(title: "제목이나 저자로 책을 찾아보세요.", description: "검색한 책은 원하는 독서 상태로 서재에 담을 수 있어요.")
        case .loading:
            Spacer()
            ProgressView("책을 찾고 있어요")
            Spacer()
        case let .results(books):
            VStack(spacing: 0) {
                HStack {
                    Text("\(books.count)개의 도서 검색 결과")
                    Spacer()
                    Text("정확도순")
                }
                .font(.caption)
                .foregroundStyle(AppTheme.secondary)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)

                List(books) { book in
                    searchResultRow(book)
                        .listRowBackground(AppTheme.background)
                        .listRowSeparatorTint(AppTheme.band)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
        case .empty:
            emptyMessage(title: "검색 결과가 없어요", description: "다른 제목이나 저자로 다시 검색해 보세요.")
        case let .failure(message):
            VStack(spacing: 14) {
                emptyMessage(title: "검색하지 못했어요", description: message)
                Button("다시 시도") { Task { await model.search() } }
                    .buttonStyle(.borderedProminent)
                    .tint(AppTheme.accent)
            }
        }
    }

    private func searchResultRow(_ book: Book) -> some View {
        HStack(alignment: .top, spacing: 12) {
            BookCover(book: book)
            VStack(alignment: .leading, spacing: 4) {
                Text(book.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(2)
                Text([book.author, book.publisher].filter { !$0.isEmpty }.joined(separator: " · "))
                    .font(.caption)
                    .foregroundStyle(AppTheme.secondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 4)
            Button {
                selectedBook = book
            } label: {
                Text(model.status(for: book) == nil ? "+ 책을 읽기 시작" : "상태 변경")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.ink)
                    .padding(.horizontal, 9)
                    .frame(height: 28)
                    .overlay {
                        RoundedRectangle(cornerRadius: 4)
                            .stroke(AppTheme.ink, lineWidth: 1)
                    }
            }
            .accessibilityLabel(model.status(for: book) == nil ? "\(book.title) 서재에 담기" : "\(book.title) 독서 상태 변경")
        }
        .padding(.vertical, 6)
    }
}

private struct LibraryView: View {
    @Environment(AppModel.self) private var model
    @State private var filter: ReadingStatus?
    @State private var pendingDeletion: LibraryBook?
    @State private var selectedLibraryBook: LibraryBook?

    private var filteredBooks: [LibraryBook] {
        guard let filter else { return model.library }
        return model.library.filter { $0.status == filter }
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("내 서재")
                    .font(.title3.weight(.bold))
                    .foregroundStyle(AppTheme.ink)
                Spacer()
                Text("\(model.library.count)권")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.secondary)
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 8)
            filters
                .padding(.bottom, 8)
            if filteredBooks.isEmpty {
                emptyLibrary
            } else {
                VStack(spacing: 0) {
                    HStack {
                        Text("나의 기록")
                        Spacer()
                        Text("최근 기록순")
                    }
                    .font(.caption)
                    .foregroundStyle(AppTheme.secondary)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)

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
        }
        .background(AppTheme.background)
        .toolbar(.hidden, for: .navigationBar)
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
        .sheet(item: $selectedLibraryBook) { libraryBook in
            StatusChangeSheet(initialStatus: libraryBook.status) { status in
                model.changeStatus(of: libraryBook, to: status)
                selectedLibraryBook = nil
            } onCancel: {
                selectedLibraryBook = nil
            }
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
            .font(.caption.weight(.semibold))
            .foregroundStyle(selected ? .white : AppTheme.secondary)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(selected ? AppTheme.ink : AppTheme.surface)
            .clipShape(Capsule())
            .overlay {
                Capsule().stroke(selected ? AppTheme.ink : AppTheme.border, lineWidth: 1)
            }
            .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private var emptyLibrary: some View {
        VStack(spacing: 12) {
            Spacer()
            Text(filter == nil ? "아직 담은 책이 없어요" : "이 상태의 책이 없어요")
                .font(.headline)
            Text("검색에서 책을 찾아 서재에 담아 보세요.")
                .font(.subheadline)
                .foregroundStyle(AppTheme.secondary)
            Button("책 검색하기") { model.selectedTab = 1 }
                .buttonStyle(.bordered)
                .tint(AppTheme.ink)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private func libraryRow(_ libraryBook: LibraryBook) -> some View {
        Button {
            selectedLibraryBook = libraryBook
        } label: {
            HStack(alignment: .top, spacing: 12) {
            BookCover(book: libraryBook.book)
            VStack(alignment: .leading, spacing: 5) {
                statusTag(libraryBook.status)
                Text(libraryBook.book.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(2)
                Text([libraryBook.book.author, libraryBook.book.publisher].filter { !$0.isEmpty }.joined(separator: " · "))
                    .font(.caption)
                    .foregroundStyle(AppTheme.secondary)
                    .lineLimit(1)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(AppTheme.secondary)
                .padding(.top, 18)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(libraryBook.book.title) 독서 상태 변경, 현재 \(libraryBook.status.title)")
        .padding(.vertical, 6)
    }

    private func statusTag(_ status: ReadingStatus) -> some View {
        Text(status.title)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(AppTheme.secondary)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .overlay { Capsule().stroke(AppTheme.border, lineWidth: 1) }
    }
}

private func pageHeader(_ title: String) -> some View {
    HStack {
        Text(title)
            .font(.title3.weight(.bold))
            .foregroundStyle(AppTheme.ink)
        Spacer()
    }
    .padding(.horizontal, 16)
    .padding(.top, 16)
    .padding(.bottom, 8)
}

private func emptyMessage(title: String, description: String) -> some View {
    VStack(spacing: 12) {
        Spacer()
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

private struct StatusChangeSheet: View {
    @State private var status: ReadingStatus
    let onConfirm: (ReadingStatus) -> Void
    let onCancel: () -> Void

    init(initialStatus: ReadingStatus, onConfirm: @escaping (ReadingStatus) -> Void, onCancel: @escaping () -> Void) {
        _status = State(initialValue: initialStatus)
        self.onConfirm = onConfirm
        self.onCancel = onCancel
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("독서 상태 변경")
                .font(.headline)
                .foregroundStyle(AppTheme.ink)
            Text("선택한 책의 독서 상태를 변경합니다.")
                .font(.caption)
                .foregroundStyle(AppTheme.secondary)
            VStack(spacing: 6) {
                ForEach(ReadingStatus.allCases) { option in
                    Button { status = option } label: {
                        HStack(spacing: 10) {
                            Image(systemName: status == option ? "largecircle.fill.circle" : "circle")
                                .foregroundStyle(status == option ? AppTheme.accent : AppTheme.secondary)
                            Text(option.title)
                            Spacer()
                        }
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.ink)
                        .padding(.horizontal, 12)
                        .frame(height: 40)
                        .background(status == option ? AppTheme.accentSoft : .clear)
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                    .buttonStyle(.plain)
                }
            }
            HStack(spacing: 8) {
                Button("취소", action: onCancel)
                    .buttonStyle(StatusSheetButtonStyle(fill: AppTheme.surface, foreground: AppTheme.ink))
                Button("변경") { onConfirm(status) }
                    .buttonStyle(StatusSheetButtonStyle(fill: AppTheme.ink, foreground: .white))
            }
        }
        .padding(20)
        .presentationDetents([.height(330)])
        .presentationDragIndicator(.hidden)
        .presentationCornerRadius(20)
        .presentationBackground(AppTheme.background)
    }
}

private struct StatusSheetButtonStyle: ButtonStyle {
    let fill: Color
    let foreground: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .background(fill.opacity(configuration.isPressed ? 0.75 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 4))
            .overlay { RoundedRectangle(cornerRadius: 4).stroke(AppTheme.ink, lineWidth: 1) }
    }
}
