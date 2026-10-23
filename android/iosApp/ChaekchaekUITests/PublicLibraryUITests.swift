import XCTest

final class PublicLibraryUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }
    @MainActor
    private func start() async throws -> XCUIApplication {
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
        app.buttons["피드"].tap()
        XCTAssertTrue(find("독자A 프로필", in: app).waitForExistence(timeout: 15))
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
        for _ in 0..<9 {
            if element.exists && element.isHittable && element.frame.minY > 90 && element.frame.maxY < app.frame.maxY - 80 { return }
            app.swipeUp()
        }
    }
    @MainActor
    private func profile(_ name: String, in app: XCUIApplication) {
        let target = find("\(name) 프로필", in: app)
        reveal(target, in: app)
        XCTAssertTrue(target.isHittable)
        target.tap()
    }
    @MainActor
    private func back(_ app: XCUIApplication) { app.buttons["뒤로 가기"].tap() }
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
}
