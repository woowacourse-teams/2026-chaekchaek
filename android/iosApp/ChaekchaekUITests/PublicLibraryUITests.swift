import XCTest

final class PublicLibraryUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }
    @MainActor
    private func start(tab: String = "피드") async throws -> XCUIApplication {
        let base = URL(string: "http://127.0.0.1:19087")!
        var probe = URLRequest(url: base.appendingPathComponent("__607/reset"))
        probe.timeoutInterval = 3
        let (data, _) = try await URLSession.shared.data(for: probe)
        XCTAssertTrue(String(data: data, encoding: .utf8)?.contains("607") == true)
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestingGuest", "-uiTestingExistingInstallation", "-uiTestingRefresh"]
        app.launchEnvironment["CHAEKCHAEK_UI_TEST_API_URL"] = base.absoluteString
        app.launch()
        XCTAssertTrue(app.buttons["피드"].waitForExistence(timeout: 20))
        app.buttons[tab].tap()
        if tab == "피드" { XCTAssertTrue(find("독자A 프로필", in: app).waitForExistence(timeout: 15)) }
        return app
    }
    @MainActor
    private func find(_ label: String, in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }
    @MainActor
    private func shelf(in app: XCUIApplication) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "기본책장")).firstMatch
    }
    @MainActor
    private func book(_ title: String, in app: XCUIApplication) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "\(title) 표지")).firstMatch
    }
    @MainActor
    private func reveal(_ element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<18 {
            let top = app.frame.minY + 150
            let bottom = app.frame.maxY - 140
            var moveUp = true
            if element.exists {
                let frame = element.frame
                if element.isHittable && frame.minY >= top && frame.maxY <= bottom {
                    // Compose의 관성 스크롤은 XCTest idle 이후에도 이동할 수 있다.
                    Thread.sleep(forTimeInterval: 0.5)
                    if element.exists && abs(element.frame.minY - frame.minY) < 1 { return }
                    continue
                }
                moveUp = frame.minY >= top
            }
            let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: moveUp ? 0.72 : 0.42))
            let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: moveUp ? 0.42 : 0.72))
            start.press(forDuration: 0.05, thenDragTo: end, withVelocity: .slow, thenHoldForDuration: 0.3)
            Thread.sleep(forTimeInterval: 0.8)
        }
        XCTFail("요소를 안전한 표시 영역으로 스크롤하지 못함: \(element)")
    }
    @MainActor
    private func profile(_ name: String, in app: XCUIApplication) {
        let target = find("\(name) 프로필", in: app)
        reveal(target, in: app)
        XCTAssertTrue(target.isHittable)
        target.tap()
    }
    @MainActor
    private func back(_ app: XCUIApplication) {
        let back = app.buttons["뒤로 가기"]
        if !back.exists || !back.isHittable {
            let top = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "TOP")).firstMatch
            if top.exists && top.isHittable { top.tap() }
        }
        XCTAssertTrue(back.waitForExistence(timeout: 10))
        back.tap()
    }
    @MainActor
    func testGuestViewerKeepsFilterAndReturnsToExistingLibrary() async throws {
        let app = try await start()
        profile("독자A", in: app)
        XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10))
        shelf(in: app).tap()
        let filter = find("읽는 중", in: app)
        XCTAssertTrue(filter.waitForExistence(timeout: 10)); reveal(filter, in: app); filter.tap()
        let firstBook = book("서재 도서 1", in: app)
        reveal(firstBook, in: app); firstBook.tap()
        XCTAssertTrue(app.staticTexts["공개 서재를 확인하는 감상"].waitForExistence(timeout: 15))
        back(app)
        XCTAssertTrue(filter.waitForExistence(timeout: 10))
        XCTAssertTrue(filter.isSelected)
        XCTAssertTrue(firstBook.exists)
        let laterBook = book("서재 도서 17", in: app)
        reveal(laterBook, in: app); laterBook.tap()
        XCTAssertTrue(app.staticTexts["공개 서재를 확인하는 감상"].waitForExistence(timeout: 15))
        back(app)
        XCTAssertTrue(laterBook.waitForExistence(timeout: 10))
        XCTAssertTrue(laterBook.isHittable)
        XCTAssertFalse(book("서재 도서 18", in: app).exists)
        laterBook.tap()
        XCTAssertTrue(app.staticTexts["공개 서재를 확인하는 감상"].waitForExistence(timeout: 15))
        profile("독자A", in: app)
        XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10))
        XCTAssertFalse(filter.exists)
        back(app)
        XCTAssertTrue(app.buttons["피드"].waitForExistence(timeout: 10))
        XCTAssertTrue(find("독자A 프로필", in: app).exists)
        let attachment = XCTAttachment(screenshot: app.screenshot()); attachment.name = "607-public-library-round-trip"; attachment.lifetime = .keepAlways; add(attachment)
    }
    @MainActor
    func testEmptyBlockedAuthorsAndRetry() async throws {
        let app = try await start()
        profile("빈서재독자", in: app)
        XCTAssertTrue(app.staticTexts["아직 담은 책이 없어요"].waitForExistence(timeout: 10)); back(app)
        profile("오류서재독자", in: app)
        XCTAssertTrue(app.staticTexts["서재를 불러오지 못했어요"].waitForExistence(timeout: 10))
        app.buttons["다시 시도"].tap()
        XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10)); back(app)
        for (name, message) in [("익명독자", "익명으로 작성한 사용자의 서재는 볼 수 없어요."),
                                ("비회원독자", "비회원으로 작성한 사용자는 서재가 없어요."),
                                ("탈퇴한 사용자", "탈퇴한 사용자의 서재는 볼 수 없어요.")] {
            profile(name, in: app)
            XCTAssertTrue(app.staticTexts[message].waitForExistence(timeout: 10))
            app.buttons["확인"].tap()
            XCTAssertTrue(app.buttons["피드"].exists)
        }
    }
    @MainActor
    func testSameBookFromAnotherOwnerKeepsIndependentDetailSort() async throws {
        let app = try await start()
        profile("독자A", in: app)
        XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10)); shelf(in: app).tap()
        let firstBook = book("서재 도서 1", in: app)
        XCTAssertTrue(firstBook.waitForExistence(timeout: 10)); reveal(firstBook, in: app); firstBook.tap()
        let latest = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "최신순")).firstMatch
        reveal(latest, in: app); XCTAssertTrue(latest.waitForExistence(timeout: 15)); latest.tap()
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", "페이지순")).firstMatch.tap()
        profile("독자B", in: app)
        let otherShelf = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "B책장")).firstMatch
        XCTAssertTrue(otherShelf.waitForExistence(timeout: 10)); otherShelf.tap()
        XCTAssertTrue(firstBook.waitForExistence(timeout: 10)); reveal(firstBook, in: app); firstBook.tap()
        reveal(latest, in: app); XCTAssertTrue(latest.waitForExistence(timeout: 15))
        XCTAssertTrue(latest.isHittable)
        back(app); XCTAssertTrue(find("읽는 중", in: app).waitForExistence(timeout: 10))
        back(app); XCTAssertTrue(otherShelf.waitForExistence(timeout: 10)); back(app)
        // 원래 상세의 저장된 위치는 B 프로필 근처다. 정렬은 위쪽에 있으므로 먼저 TOP으로 이동한다.
        let top = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "TOP")).firstMatch
        if top.exists && top.isHittable {
            top.tap()
            XCTAssertTrue(app.buttons["뒤로 가기"].waitForExistence(timeout: 10))
        }
        let originalSort = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "페이지순")).firstMatch
        reveal(originalSort, in: app); XCTAssertTrue(originalSort.waitForExistence(timeout: 15))
        XCTAssertTrue(originalSort.isHittable)
    }

    @MainActor
    private func requestCount(memberId: Int) async throws -> Int {
        let (data, _) = try await URLSession.shared.data(from: URL(string: "http://127.0.0.1:19087/__607")!)
        let body = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        let counts = try XCTUnwrap(body["counts"] as? [String: Int])
        return try XCTUnwrap(counts["/api/v1/libraries?memberId=\(memberId)"])
    }
    @MainActor
    private func dragSpines(_ app: XCUIApplication, y: CGFloat) {
        let start = app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: app.frame.width * 0.85, dy: y))
        let end = app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: app.frame.width * 0.15, dy: y))
        start.press(forDuration: 0.05, thenDragTo: end, withVelocity: .slow, thenHoldForDuration: 0.3)
        Thread.sleep(forTimeInterval: 1)
    }
    @MainActor
    func testHomeAndDiscoverProfilesRestoreOriginalTabAndPosition() async throws {
        let app = try await start(tab: "홈")
        let author = find("독자A 프로필", in: app)
        reveal(author, in: app); let homeY = author.frame.minY
        author.tap(); XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10)); back(app)
        XCTAssertTrue(app.buttons["홈"].isSelected)
        XCTAssertEqual(author.frame.minY, homeY, accuracy: 1)
        app.buttons["발견"].tap()
        reveal(author, in: app); let discoverY = author.frame.minY
        author.tap(); XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10)); back(app)
        XCTAssertTrue(app.buttons["발견"].isSelected)
        XCTAssertEqual(author.frame.minY, discoverY, accuracy: 1)
        let attachment = XCTAttachment(screenshot: app.screenshot()); attachment.name = "607-home-discover"; attachment.lifetime = .keepAlways; add(attachment)
    }
    @MainActor
    func testReplyProfileReusesExistingLibraryAndRapidBackKeepsRoot() async throws {
        let app = try await start()
        profile("독자A", in: app); XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10)); shelf(in: app).tap()
        let firstBook = book("서재 도서 1", in: app)
        XCTAssertTrue(firstBook.waitForExistence(timeout: 10)); reveal(firstBook, in: app); firstBook.tap()
        let replyAuthor = find("답글독자 프로필", in: app)
        reveal(replyAuthor, in: app); replyAuthor.doubleTap()
        XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["독자A"].exists)
        back(app); XCTAssertTrue(app.buttons["피드"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["피드"].isSelected)
        profile("독자A", in: app); XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10)); shelf(in: app).doubleTap()
        XCTAssertTrue(find("읽는 중", in: app).waitForExistence(timeout: 10))
        let before = XCTAttachment(screenshot: app.screenshot()); before.name = "607-before-edge-gesture"; before.lifetime = .keepAlways; add(before)
        // 짧고 느린 드래그는 취소하고, 다음 긴 스와이프는 완료한다.
        let edge = app.coordinate(withNormalizedOffset: CGVector(dx: 0.005, dy: 0.5))
        let middle = app.coordinate(withNormalizedOffset: CGVector(dx: 0.12, dy: 0.5))
        edge.press(forDuration: 0.05, thenDragTo: middle, withVelocity: XCUIGestureVelocity(rawValue: 25), thenHoldForDuration: 0.3)
        let after = XCTAttachment(screenshot: app.screenshot()); after.name = "607-after-edge-gesture"; after.lifetime = .keepAlways; add(after)
        XCTAssertTrue(find("읽는 중", in: app).waitForExistence(timeout: 10))
        app.buttons["뒤로 가기"].doubleTap()
        XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10))
        let completed = app.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.5))
        edge.press(forDuration: 0.05, thenDragTo: completed, withVelocity: .slow, thenHoldForDuration: 0.3)
        XCTAssertTrue(app.buttons["피드"].waitForExistence(timeout: 10))
        app.buttons["발견"].tap(); XCTAssertTrue(app.buttons["발견"].isSelected)
        // 정상 종료 후 재실행에도 루트 탭이 표시되고 프로필 진입이 다시 가능하다.
        app.terminate(); app.launch(); XCTAssertTrue(app.buttons["피드"].waitForExistence(timeout: 20)); app.buttons["피드"].tap()
        profile("독자A", in: app); XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10)); back(app)
    }
    @MainActor
    func testHorizontalSpinesEmptyBookshelfAndErrorClose() async throws {
        let app = try await start()
        profile("독자A", in: app); XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10))
        let libraryShelf = shelf(in: app)
        let originalLabel = libraryShelf.label
        dragSpines(app, y: libraryShelf.frame.minY + 160)
        let scrolledLabel = libraryShelf.label
        XCTAssertNotEqual(scrolledLabel, originalLabel)
        let librarySpine = find("서재 도서 17 책등", in: app)
        XCTAssertTrue(librarySpine.exists)
        let libraryX = librarySpine.frame.minX
        libraryShelf.tap(); XCTAssertTrue(find("읽는 중", in: app).waitForExistence(timeout: 10))
        let firstSpine = find("서재 도서 1 책등", in: app)
        XCTAssertTrue(firstSpine.waitForExistence(timeout: 10))
        dragSpines(app, y: firstSpine.frame.midY)
        let laterSpine = find("서재 도서 17 책등", in: app)
        XCTAssertTrue(laterSpine.exists); XCTAssertTrue(laterSpine.isHittable)
        let shelfX = laterSpine.frame.minX
        laterSpine.tap(); XCTAssertTrue(app.staticTexts["공개 서재를 확인하는 감상"].waitForExistence(timeout: 15)); back(app)
        XCTAssertTrue(laterSpine.waitForExistence(timeout: 10)); XCTAssertEqual(laterSpine.frame.minX, shelfX, accuracy: 1)
        back(app); XCTAssertTrue(libraryShelf.waitForExistence(timeout: 10)); XCTAssertEqual(libraryShelf.label, scrolledLabel)
        XCTAssertTrue(librarySpine.exists); XCTAssertEqual(librarySpine.frame.minX, libraryX, accuracy: 1)
        let empty = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "빈책장")).firstMatch
        reveal(empty, in: app); empty.tap()
        XCTAssertTrue(find("전체", in: app).waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["아직 담은 책이 없어요"].exists)
        back(app); XCTAssertTrue(libraryShelf.waitForExistence(timeout: 10)); back(app)
        profile("오류서재독자", in: app); XCTAssertTrue(app.staticTexts["서재를 불러오지 못했어요"].waitForExistence(timeout: 10))
        app.buttons["닫기"].tap(); XCTAssertFalse(app.staticTexts["서재를 불러오지 못했어요"].exists)
        XCTAssertTrue(app.staticTexts["오류서재독자"].exists)
        let countAfterClose = try await requestCount(memberId: 44); XCTAssertEqual(countAfterClose, 1)
        back(app)
        XCTAssertTrue(find("오류서재독자 프로필", in: app).exists)
        profile("오류서재독자", in: app); XCTAssertTrue(shelf(in: app).waitForExistence(timeout: 10))
        let countAfterReentry = try await requestCount(memberId: 44); XCTAssertEqual(countAfterReentry, 2)
    }
}
