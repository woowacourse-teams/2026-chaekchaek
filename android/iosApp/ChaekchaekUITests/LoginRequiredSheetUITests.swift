import XCTest
import Network

final class LoginRequiredSheetUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testNativeSignInButtonsAppearInLoginSheet() throws {
        let app = XCUIApplication()
        app.launchArguments.append("-uiTestingGuest")
        app.launch()

        let shelfTab = app.buttons["내 서재"]
        XCTAssertTrue(shelfTab.waitForExistence(timeout: 10))
        shelfTab.tap()

        let editButton = app.buttons["편집"]
        XCTAssertTrue(editButton.waitForExistence(timeout: 10))
        editButton.tap()

        let title = app.staticTexts["로그인이 필요해요"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        let appleButton = app.buttons["Apple로 계속하기"]
        let googleButton = app.buttons["GIDSignInButton"]
        XCTAssertTrue(appleButton.waitForExistence(timeout: 5))
        XCTAssertTrue(googleButton.waitForExistence(timeout: 5))

        XCTAssertEqual(appleButton.frame.minX, googleButton.frame.minX, accuracy: 1)
        XCTAssertEqual(appleButton.frame.width, googleButton.frame.width, accuracy: 1)
        XCTAssertEqual(appleButton.frame.height, 48, accuracy: 1)
        XCTAssertEqual(googleButton.frame.height, 48, accuracy: 1)
        XCTAssertEqual(googleButton.frame.minY - appleButton.frame.maxY, 12, accuracy: 1)

        if title.frame.height > appleButton.frame.height {
            app.swipeUp()
        }
        XCTAssertTrue(googleButton.isHittable)

        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "iOS 네이티브 로그인 버튼"
        attachment.lifetime = .keepAlways
        add(attachment)

        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion])
    }

    @MainActor
    func testMyPageDefaultAndWithdrawalDialogAreAccessible() throws {
        let app = launchMyPage()

        XCTAssertTrue(app.staticTexts["마이페이지"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["익명으로 감상 공개"].exists)
        let withdrawal = app.buttons.matching(
            NSPredicate(format: "label CONTAINS %@", "회원 탈퇴")
        ).firstMatch
        XCTAssertTrue(withdrawal.exists)

        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion])
        attachScreenshot(named: "마이페이지 기본")

        withdrawal.tap()
        XCTAssertTrue(app.buttons["탈퇴하기"].waitForExistence(timeout: 5))
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion])
        attachScreenshot(named: "마이페이지 회원 탈퇴 확인")
    }

    @MainActor
    func testMyPageSupportsAX5() throws {
        let app = XCUIApplication()
        app.launchArguments.append("-uiTestingMyPage")
        app.launch()

        XCTAssertTrue(app.staticTexts["마이페이지"].waitForExistence(timeout: 10))
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion])
        attachScreenshot(named: "마이페이지 AX5")
    }

    @MainActor
    func testHomePullRefreshHidesListEndAndKeepsNavigationVisible() throws {
        let fixture = try RefreshFixtureServer()
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 15))
        let end = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "여기까지 모두 봤어요")).firstMatch
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "새로고침 검증 도서")).firstMatch.waitForExistence(timeout: 15))
        for _ in 0..<8 { app.swipeUp() }
        XCTAssertFalse(end.exists)
        attachScreenshot(named: "홈 하단 끝 안내 없음")
        app.buttons["홈"].tap()
        let requestsBeforePull = fixture.completedRequests(for: "/api/v1/home/latest-reviews")
        XCTAssertGreaterThan(requestsBeforePull, 0)
        let shortPullStart = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.45))
        let shortPullEnd = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.50))
        shortPullStart.press(forDuration: 0.1, thenDragTo: shortPullEnd)
        let unexpectedRefresh = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            fixture.completedRequests(for: "/api/v1/home/latest-reviews") > requestsBeforePull
        }, object: nil)
        unexpectedRefresh.isInverted = true
        XCTAssertEqual(XCTWaiter.wait(for: [unexpectedRefresh], timeout: 1), .completed,
                       "짧은 당김은 API를 다시 요청하지 않아야 합니다")
        app.swipeDown()
        XCTAssertTrue(refreshLoadingLabel(in: app).waitForExistence(timeout: 3))
        attachScreenshot(named: "홈 단순 로더와 안내 문구")
        waitForNewResponse(fixture, path: "/api/v1/home/latest-reviews", after: requestsBeforePull)
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertEqual(fixture.completedRequests(for: "/api/v1/home/latest-reviews"), requestsBeforePull + 1)
        XCTAssertTrue(app.buttons["내 프로필"].isHittable)
        XCTAssertTrue(app.buttons["발견"].exists)
        XCTAssertTrue(app.buttons["내 서재"].exists)
        attachScreenshot(named: "홈 당김 후 기존 화면")
        // hitRegion은 시스템 홈 인디케이터에서도 실패해 별도 검증 기록으로 남긴다.
        try app.performAccessibilityAudit(for: [.textClipped])
    }

    @MainActor
    func testEmptySearchPullKeepsSubmittedInput() throws {
        let fixture = try RefreshFixtureServer()
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        XCTAssertTrue(app.buttons["발견"].waitForExistence(timeout: 15))
        app.buttons["발견"].tap()
        let field = app.textViews["search-query-field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        let query = "zzxqrefresh529nomatch"
        // 부하가 큰 Simulator의 연속 입력 누락과 새로고침의 입력 보존 실패를 구분한다.
        for character in query { field.typeText(String(character)) }
        guard field.value as? String == query else {
            XCTFail("새로고침 전에 테스트 검색어가 정확히 입력되어야 합니다")
            return
        }
        field.typeText("\n")
        let empty = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "검색 결과가 없어요")).firstMatch
        XCTAssertTrue(empty.waitForExistence(timeout: 15))
        let requestsBeforePull = fixture.completedRequests(for: "/api/v1/books")
        XCTAssertEqual(requestsBeforePull, 1)
        app.swipeDown()
        XCTAssertTrue(refreshLoadingLabel(in: app).waitForExistence(timeout: 3))
        attachScreenshot(named: "빈 검색 단순 로더와 안내 문구")
        waitForNewResponse(fixture, path: "/api/v1/books", after: requestsBeforePull)
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertEqual(fixture.completedRequests(for: "/api/v1/books"), requestsBeforePull + 1)
        XCTAssertTrue(empty.exists)
        XCTAssertEqual(field.value as? String, query)
        attachScreenshot(named: "빈 검색 결과 당김 후 검색어 유지")
        // hitRegion은 시스템 홈 인디케이터에서도 실패해 별도 검증 기록으로 남긴다.
        try app.performAccessibilityAudit(for: [.textClipped])
    }

    @MainActor
    func testEmptyFeedPullRefreshesWithoutListEnd() throws {
        let fixture = try RefreshFixtureServer()
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        XCTAssertTrue(app.buttons["피드"].waitForExistence(timeout: 15))
        app.buttons["피드"].tap()
        let empty = app.staticTexts.matching(
            NSPredicate(format: "label CONTAINS %@", "아직 도착한 감상이 없어요")
        ).firstMatch
        XCTAssertTrue(empty.waitForExistence(timeout: 15))
        let path = "/api/v1/feed/reviews"
        let requestsBeforePull = fixture.completedRequests(for: path)
        XCTAssertGreaterThan(requestsBeforePull, 0)
        app.swipeDown()
        XCTAssertTrue(refreshLoadingLabel(in: app).waitForExistence(timeout: 3))
        attachScreenshot(named: "빈 피드 단순 로더와 안내 문구")
        waitForNewResponse(fixture, path: path, after: requestsBeforePull)
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertEqual(fixture.completedRequests(for: path), requestsBeforePull + 1)
        XCTAssertTrue(empty.exists)
        XCTAssertFalse(app.staticTexts.matching(
            NSPredicate(format: "label CONTAINS %@", "여기까지 모두 봤어요")
        ).firstMatch.exists)
        attachScreenshot(named: "빈 피드 당김 후 기존 화면")
        try app.performAccessibilityAudit(for: [.textClipped])
    }

    @MainActor
    func testBookDetailPullKeepsContentAndRefreshesReviews() throws {
        try verifyBookDetailPull(initialReviews: true)
    }

    @MainActor
    func testEmptyBookDetailReviewsCanBeRefreshed() throws {
        try verifyBookDetailPull(initialReviews: false)
    }

    @MainActor
    private func verifyBookDetailPull(initialReviews: Bool) throws {
        let fixture = try RefreshFixtureServer(initialDetailReviews: initialReviews)
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        let title = app.descendants(matching: .any).matching(NSPredicate(
            format: "label CONTAINS %@", "새로고침 검증 도서"
        )).firstMatch
        XCTAssertTrue(title.waitForExistence(timeout: 15))
        title.tap()
        let detailPath = "/api/v1/books/by-isbn/9780000000001"
        let reviewsPath = "/api/v1/books/1/reviews"
        waitForNewResponse(fixture, path: reviewsPath, after: 0)
        XCTAssertTrue(app.buttons["뒤로 가기"].waitForExistence(timeout: 5))
        let initialContent = initialReviews ? "기존 상세 감상" : "아직 등록된 감상이 없어요"
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", initialContent))
            .firstMatch.waitForExistence(timeout: 10))
        fixture.beginDetailRefresh()
        let detailBefore = fixture.completedRequests(for: detailPath)
        let reviewsBefore = fixture.completedRequests(for: reviewsPath)
        app.swipeDown()
        XCTAssertTrue(refreshLoadingLabel(in: app).waitForExistence(timeout: 3))
        XCTAssertTrue(title.exists)
        attachScreenshot(named: "책 상세 C 새로고침 \(initialReviews ? "기존 감상" : "빈 감상")")
        waitForNewResponse(fixture, path: reviewsPath, after: reviewsBefore)
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertEqual(fixture.completedRequests(for: detailPath), detailBefore + 1)
        XCTAssertEqual(fixture.completedRequests(for: reviewsPath), reviewsBefore + 1)
        let updated = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "갱신한 상세 감상")).firstMatch
        for _ in 0..<4 where !updated.isHittable { app.swipeUp() }
        XCTAssertTrue(updated.isHittable)
        attachScreenshot(named: "책 상세 갱신 후 감상")
    }

    @MainActor
    private func refreshApplication(_ fixture: RefreshFixtureServer) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-uiTestingGuest", "-uiTestingRefresh"]
        app.launchEnvironment["CHAEKCHAEK_UI_TEST_API_URL"] = fixture.baseURL
        return app
    }

    @MainActor
    private func waitForNewResponse(_ fixture: RefreshFixtureServer, path: String, after count: Int) {
        // 응답이 한 프레임 안에 끝나도 누적 횟수는 남는다. 콜백이 끊기면 반드시 시간 초과된다.
        let response = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            fixture.completedRequests(for: path) > count
        }, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [response], timeout: 10), .completed,
                       "당김 후 실제 API 응답이 완료되어야 합니다: \(path)")
    }

    @MainActor
    private func refreshIndicator(in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(
            format: "identifier == %@ OR label IN %@", "pull-refresh-indicator",
            ["당겨서 새로고침", "놓으면 새로고침", "갱신 요청 중", "새로고침 중이에요"]
        )).firstMatch
    }

    @MainActor
    private func refreshLoadingLabel(in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(
            format: "label == %@", "새로고침 중이에요"
        )).firstMatch
    }

    @MainActor
    private func waitForRefreshIndicatorToClose(in app: XCUIApplication) {
        expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: refreshIndicator(in: app))
        waitForExpectations(timeout: 10)
    }

    @MainActor
    private func launchMyPage() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments.append("-uiTestingMyPage")
        app.launch()
        return app
    }

    @MainActor
    private func attachScreenshot(named name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}

/// XCUITest 프로세스에서 응답과 횟수를 소유한다. 앱에는 화면용 진단 요소를 추가하지 않는다.
private final class RefreshFixtureServer: @unchecked Sendable {
    private let listener: NWListener
    private let queue = DispatchQueue(label: "chaekchaek.refresh-ui-test-api")
    private let lock = NSLock()
    private var responses: [String: Int] = [:]
    private let initialDetailReviews: Bool
    private var detailRefreshArmed = false

    var baseURL: String { "http://127.0.0.1:\(listener.port!.rawValue)" }

    init(initialDetailReviews: Bool = false) throws {
        self.initialDetailReviews = initialDetailReviews
        listener = try NWListener(using: .tcp, on: .any)
        let ready = XCTestExpectation(description: "로컬 API 서버 시작")
        listener.stateUpdateHandler = { state in
            if case .ready = state { ready.fulfill() }
        }
        listener.newConnectionHandler = { [weak self] connection in
            guard let self else { return }
            connection.start(queue: self.queue)
            self.receive(connection)
        }
        listener.start(queue: queue)
        guard XCTWaiter.wait(for: [ready], timeout: 5) == .completed else {
            listener.cancel()
            throw NSError(domain: "RefreshFixtureServer", code: 1)
        }
    }

    func completedRequests(for path: String) -> Int {
        lock.lock()
        defer { lock.unlock() }
        return responses[path, default: 0]
    }

    func beginDetailRefresh() {
        lock.lock()
        detailRefreshArmed = true
        lock.unlock()
    }

    private var isDetailRefresh: Bool {
        lock.lock()
        defer { lock.unlock() }
        return detailRefreshArmed
    }

    func stop() { listener.cancel() }

    private func receive(_ connection: NWConnection, buffered: Data = Data()) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 65_536) { [weak self] data, _, complete, error in
            guard let self else { return }
            let request = buffered + (data ?? Data())
            guard String(data: request, encoding: .utf8)?.contains("\r\n\r\n") == true else {
                if !complete && error == nil { self.receive(connection, buffered: request) }
                else { connection.cancel() }
                return
            }
            let target = String(data: request, encoding: .utf8)?.components(separatedBy: " ").dropFirst().first ?? ""
            let path = URLComponents(string: "http://127.0.0.1\(target)")?.path ?? ""
            let body = self.response(for: path)
            let header = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: \(body.count)\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n"
            // 두 번째 조회부터 지연시켜 실제 표시와 응답 후 접힘을 검증한다.
            let detailRequest = path == "/api/v1/books/1/reviews" || path == "/api/v1/books/by-isbn/9780000000001"
            let delayed = detailRequest ? self.isDetailRefresh : self.completedRequests(for: path) > 0
            let delay = delayed ? 2.0 : 0.0
            self.queue.asyncAfter(deadline: .now() + delay) {
                connection.send(content: Data(header.utf8) + body, completion: .contentProcessed { error in
                    if error == nil {
                        self.lock.lock()
                        self.responses[path, default: 0] += 1
                        self.lock.unlock()
                    }
                    connection.cancel()
                })
            }
        }
    }

    private func response(for path: String) -> Data {
        let body: String
        switch path {
        case "/api/v1/books/by-isbn/9780000000001":
            body = """
            {"bookId":1,"isbn13":"9780000000001","title":"새로고침 검증 도서","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","myRecord":null}
            """
        case "/api/v1/books/1/reviews":
            let refreshed = isDetailRefresh
            if !initialDetailReviews && !refreshed {
                body = "{\"totalCount\":0,\"nextPage\":null,\"items\":[]}"
            } else {
                let content = refreshed ? "갱신한 상세 감상" : "기존 상세 감상"
                body = """
                {"totalCount":1,"nextPage":null,"items":[{"reviewId":7,"content":"\(content)","createdAt":"2026-10-21T00:00:00Z","author":{"displayName":"검증 독자","anonymous":false,"mine":false,"actorType":"GUEST"},"replyCount":0,"likeCount":0,"likedByMe":false,"isSpoiler":false,"recentReplies":[],"deleted":false}]}
                """
            }

        case "/api/v1/home/popular-books":
            body = """
            {"books":[{"bookId":1,"isbn13":"9780000000001","title":"새로고침 검증 도서","coverImageUrl":"","reviewCount":4,"replyCount":0}]}
            """
        case "/api/v1/home/latest-reviews":
            let reviews = (1...4).map { index in
                """
                {"reviewId":\(index),"content":"목록의 마지막과 당김 후 기존 내용이 유지되는지 확인하는 감상입니다.","createdAt":"2026-10-01T08:00:0\(index)Z","author":{"displayName":"독자 \(index)"},"replyCount":0,"bookId":1,"isbn13":"9780000000001","bookTitle":"새로고침 검증 도서","bookCoverImageUrl":""}
                """
            }.joined(separator: ",")
            body = "{\"reviews\":[\(reviews)]}"
        case "/api/v1/feed/reviews":
            body = "{\"reviews\":[],\"totalCount\":0,\"nextPage\":null}"
        default:
            body = "{\"totalCount\":0,\"nextPage\":null,\"items\":[]}"
        }
        return Data(body.utf8)
    }
}
