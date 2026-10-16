import XCTest
import Network

final class LoginRequiredSheetUITests: XCTestCase {
    // CHAEKCHAEK_API_ENVIRONMENT=local 빌드와 실패 후 성공하는 로컬 fixture가 필요하다.
    @MainActor
    func testReviewDraftSurvivesFailureAndClosesAfterRetry() async throws {
        let base = URL(string: "http://127.0.0.1:9090")!
        guard let (data, _) = try? await URLSession.shared.data(from: base.appendingPathComponent("__review-draft-qa")),
              String(data: data, encoding: .utf8)?.contains("review-draft") == true else {
            throw XCTSkip("감상 보존 전용 로컬 fixture가 실행 중일 때만 검증한다")
        }
        var reset = URLRequest(url: base.appendingPathComponent("__review-draft-qa/reset"))
        reset.httpMethod = "POST"
        _ = try await URLSession.shared.data(for: reset)

        let app = XCUIApplication()
        app.launchArguments.append("-uiTestingGuest")
        app.launch()
        let book = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "감상 보존 검증")).firstMatch
        guard book.waitForExistence(timeout: 15) else {
            XCTFail("로컬 fixture 도서가 표시되지 않아 검증을 중단한다")
            return
        }
        book.tap()
        let open = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "이 순간의 감상 남기기")).firstMatch
        XCTAssertTrue(open.waitForExistence(timeout: 10))
        open.tap()
        let content = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 (필수)")).firstMatch
        guard content.waitForExistence(timeout: 10) else {
            XCTFail("감상 입력란 접근 실패: \(app.debugDescription)")
            return
        }
        // 아래 필드부터 채워 키보드가 펼쳐질 때 입력란을 가리는 영향을 줄인다.
        let chapter = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "목차 또는 챕터")).firstMatch
        revealReviewControl(chapter, in: app, scrollingUp: true)
        chapter.tap()
        chapter.typeText("chapter one")
        let quote = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "인상 깊은 문구")).firstMatch
        revealReviewControl(quote, in: app, scrollingUp: false)
        quote.tap()
        quote.typeText("saved quote")
        revealReviewControl(content, in: app, scrollingUp: false)
        content.tap()
        content.typeText("draft must survive")
        let spoiler = app.buttons["스포일러"]
        revealReviewControl(spoiler, in: app, scrollingUp: true)
        spoiler.tap()
        let save = app.buttons["감상 남기기"]
        revealReviewControl(save, in: app, scrollingUp: true)
        try await Task.sleep(for: .milliseconds(500))
        save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).press(forDuration: 0.15)
        // 첫 요청은 500으로 실패한다. 응답 뒤에도 같은 작성창과 입력이 남아야 한다.
        try await Task.sleep(for: .seconds(2))
        let (attemptData, _) = try await URLSession.shared.data(from: base.appendingPathComponent("__review-draft-qa/state"))
        let attempts = try XCTUnwrap(JSONSerialization.jsonObject(with: attemptData) as? [String: [[String: Any]]])
        guard attempts["requests"]?.count == 1 else {
            attachScreenshot(named: "감상 저장 버튼 입력 확인")
            XCTFail("첫 저장 요청이 전달되지 않았다: \(save.debugDescription)")
            return
        }
        guard content.exists else {
            attachScreenshot(named: "수정 전 저장 실패로 작성창이 닫힘")
            XCTFail("저장 실패 뒤 작성창과 입력이 사라졌다")
            return
        }
        revealReviewControl(content, in: app, scrollingUp: false)
        XCTAssertEqual(content.value as? String, "draft must survive")
        XCTAssertEqual(quote.value as? String, "saved quote")
        XCTAssertEqual(chapter.value as? String, "chapter one")
        attachScreenshot(named: "감상 저장 실패 후 입력 보존")
        revealReviewControl(save, in: app, scrollingUp: true)
        try await Task.sleep(for: .milliseconds(500))
        save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).press(forDuration: 0.15)
        let closed = NSPredicate(format: "exists == false")
        let closedExpectation = expectation(for: closed, evaluatedWith: content)
        await fulfillment(of: [closedExpectation], timeout: 10)
        let (savedData, _) = try await URLSession.shared.data(from: base.appendingPathComponent("__review-draft-qa/state"))
        let saved = try XCTUnwrap(JSONSerialization.jsonObject(with: savedData) as? [String: [[String: Any]]])
        XCTAssertEqual(saved["requests"]?.count, 2)
        let submissions = try XCTUnwrap(saved["requests"])
        XCTAssertEqual(submissions.first?["content"] as? String, "draft must survive")
        XCTAssertEqual(submissions.last?["quote"] as? String, "saved quote")
        XCTAssertEqual(submissions.last?["chapter"] as? String, "chapter one")
        XCTAssertEqual(submissions.last?["isSpoiler"] as? Bool, true)
        let first = try XCTUnwrap(submissions.first)
        let last = try XCTUnwrap(submissions.last)
        XCTAssertTrue(NSDictionary(dictionary: first).isEqual(to: last))
        attachScreenshot(named: "재시도 성공 후 작성창 닫힘")
        open.tap()
        XCTAssertTrue(content.waitForExistence(timeout: 5))
        XCTAssertTrue((content.value as? String ?? "").isEmpty)
        attachScreenshot(named: "새 감상 작성창 초기화")
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion])
    }

    @MainActor
    private func revealReviewControl(_ control: XCUIElement, in app: XCUIApplication, scrollingUp: Bool) {
        for _ in 0..<5 {
            let keyboard = app.keyboards.firstMatch
            let bottom = keyboard.exists ? keyboard.frame.minY - 100 : app.frame.maxY - 34
            if control.exists && control.isHittable && control.frame.height >= 40 && control.frame.maxY <= bottom {
                return
            }
            // 텍스트 입력란과 키보드가 화면 스와이프를 소비하므로 시트의 빈 오른쪽 여백을 이용한다.
            let top = max(160, bottom - 300)
            let origin = app.coordinate(withNormalizedOffset: .zero)
            let start = origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: scrollingUp ? bottom - 24 : top))
            let end = origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: scrollingUp ? top : bottom - 24))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
    }

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
    func testHomePullRefreshAndListEndKeepNavigationVisible() throws {
        let fixture = try RefreshFixtureServer()
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 15))
        let end = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "여기까지 모두 봤어요")).firstMatch
        for _ in 0..<8 {
            if end.exists && end.isHittable { break }
            app.swipeUp()
        }
        XCTAssertTrue(end.waitForExistence(timeout: 10))
        attachScreenshot(named: "홈 목록 끝")
        app.buttons["홈"].tap()
        let requestsBeforePull = fixture.completedRequests(for: "/api/v1/home/latest-reviews")
        XCTAssertGreaterThan(requestsBeforePull, 0)
        app.swipeDown()
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
        field.typeText("zzxqrefresh529nomatch\n")
        let empty = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "검색 결과가 없어요")).firstMatch
        XCTAssertTrue(empty.waitForExistence(timeout: 15))
        let requestsBeforePull = fixture.completedRequests(for: "/api/v1/books")
        XCTAssertEqual(requestsBeforePull, 1)
        app.swipeDown()
        waitForNewResponse(fixture, path: "/api/v1/books", after: requestsBeforePull)
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertEqual(fixture.completedRequests(for: "/api/v1/books"), requestsBeforePull + 1)
        XCTAssertTrue(empty.exists)
        XCTAssertEqual(field.value as? String, "zzxqrefresh529nomatch")
        attachScreenshot(named: "빈 검색 결과 당김 후 검색어 유지")
        // hitRegion은 시스템 홈 인디케이터에서도 실패해 별도 검증 기록으로 남긴다.
        try app.performAccessibilityAudit(for: [.textClipped])
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
    private func waitForRefreshIndicatorToClose(in app: XCUIApplication) {
        let indicator = app.staticTexts.matching(NSPredicate(
            format: "label CONTAINS %@ OR label CONTAINS %@ OR label CONTAINS %@ OR label CONTAINS %@",
            "당겨서 새로고침", "놓으면 새로고침", "갱신 요청 중", "새로고침 중이에요"
        )).firstMatch
        expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: indicator)
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

    var baseURL: String { "http://127.0.0.1:\(listener.port!.rawValue)" }

    init() throws {
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

    private func response(for path: String) -> Data {
        let body: String
        switch path {
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
        default:
            body = "{\"totalCount\":0,\"nextPage\":null,\"items\":[]}"
        }
        return Data(body.utf8)
    }
}
