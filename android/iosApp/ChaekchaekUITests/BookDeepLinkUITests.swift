import XCTest

final class BookDeepLinkUITests: XCTestCase {
    private let base = URL(string: "http://127.0.0.1:19092")!
    private let valid = URL(string: "chaekchaek://books/9788937460449")!
    override func setUpWithError() throws { continueAfterFailure = false }

    @MainActor
    private func prepared(onboarding: Bool = false) async throws -> XCUIApplication {
        let (data, _) = try await URLSession.shared.data(from: base.appendingPathComponent("__692/reset"))
        XCTAssertTrue(String(decoding: data, as: UTF8.self).contains("692"))
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestingRefresh", "-uiTestingBookDeepLink", onboarding ? "-uiTestingResetOnboarding" : "-uiTestingExistingInstallation"]
        app.launchEnvironment["CHAEKCHAEK_UI_TEST_API_URL"] = base.absoluteString
        return app
    }

    @MainActor private func detail(_ app: XCUIApplication) {
        XCTAssertTrue(app.staticTexts["딥링크 검증 도서"].waitForExistence(timeout: 15), app.debugDescription)
    }

    @MainActor private func back(_ app: XCUIApplication) {
        let button = app.buttons["book_detail_back"]
        if button.waitForExistence(timeout: 5) {
            button.tap()
            return
        }
        let fallback = app.buttons["뒤로 가기"]
        XCTAssertTrue(fallback.waitForExistence(timeout: 5))
        fallback.tap()
    }

    @MainActor private func capture(_ app: XCUIApplication, _ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    @MainActor private func home(_ app: XCUIApplication) {
        let homeTab = app.buttons["홈"]
        XCTAssertTrue(homeTab.waitForExistence(timeout: 5))
        XCTAssertTrue(homeTab.isSelected)
    }

    @MainActor func testColdLinkOpensDetailAndReturnsHome() async throws {
        let app = try await prepared()
        app.open(valid)
        detail(app)
        capture(app, "딥링크 상세")
        try await assertOneInitialLookup()
        back(app)
        home(app)
    }

    @MainActor private func assertOneInitialLookup() async throws {
        for _ in 0..<30 {
            let (data, _) = try await URLSession.shared.data(from: base.appendingPathComponent("__692"))
            let response = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
            let counts = try XCTUnwrap(response["counts"] as? [String: Int])
            if counts["/api/v1/books/692/reviews", default: 0] > 0 {
                XCTAssertEqual(counts["/api/v1/books/by-isbn/9788937460449"], 1)
                return
            }
            try await Task.sleep(for: .milliseconds(200))
        }
        XCTFail("딥링크에서 연 상세의 감상 조회가 시작되지 않았어요.")
    }

    @MainActor func testWarmLinkReturnsToDiscoverWithInput() async throws {
        let app = try await prepared()
        app.launch()
        let discover = app.buttons["발견"]
        XCTAssertTrue(discover.waitForExistence(timeout: 10))
        discover.tap()
        let field = app.textViews["search-query-field"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        let query = "linkdraft"
        for character in query { field.typeText(String(character)) }
        XCTAssertEqual(field.value as? String, query)
        try await deliverToRunningSimulator()
        detail(app)
        back(app)
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        XCTAssertEqual(field.value as? String, query)
        XCTAssertTrue(discover.isSelected)

        XCUIDevice.shared.press(.home)
        try await deliverToRunningSimulator()
        detail(app)
        back(app)
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        XCTAssertEqual(field.value as? String, query)
    }

    @MainActor private func deliverToRunningSimulator() async throws {
        let (_, response) = try await URLSession.shared.data(from: base.appendingPathComponent("__692/open-ios"))
        let status = (response as? HTTPURLResponse)?.statusCode
        if status == 501 { throw XCTSkip("fixture 실행 시 --ios-simulator UDID가 필요합니다") }
        XCTAssertEqual(status, 200)
        // 앱 밖에서 연 사용자 지정 scheme은 시스템이 "'책췍'에서 열겠습니까?"를 묻기도 한다.
        // 이 창은 앱이 아닌 SpringBoard에 있어 앱 요소 트리에 나타나지 않고, 실행마다 나타나지 않을 수도 있다.
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let open = springboard.buttons.matching(NSPredicate(format: "label IN %@", ["열기", "Open"])).firstMatch
        if open.waitForExistence(timeout: 3) { open.tap() }
    }

    @MainActor func testOnboardingDefersLinkUntilCompleted() async throws {
        let app = try await prepared(onboarding: true)
        app.open(valid)
        XCTAssertTrue(app.buttons["건너뛰기"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.staticTexts["딥링크 검증 도서"].exists)
        app.buttons["건너뛰기"].tap()
        detail(app)
        capture(app, "온보딩 후 상세 재개")
        back(app)
        home(app)
    }

    @MainActor func testDeveloperToolExecutesLinksAndPreservesInput() async throws {
        let app = try await prepared()
        app.launch()
        XCTAssertTrue(app.buttons["개발"].waitForExistence(timeout: 15))
        app.buttons["개발"].tap()
        XCTAssertTrue(app.staticTexts["책 상세 딥링크"].waitForExistence(timeout: 5))
        capture(app, "개발자 도구 딥링크")

        // 1. 잘못된 링크 실행 -> 오류 안내 후 닫기 -> 입력값 보존
        app.buttons["잘못된 링크"].tap()
        app.buttons["링크 실행"].tap()
        XCTAssertTrue(app.staticTexts["링크를 열 수 없어요"].waitForExistence(timeout: 5))
        app.buttons["확인"].tap()
        let uri = app.textViews["developer-deeplink-uri"]
        XCTAssertEqual(uri.value as? String, "chaekchaek://books/not-a-book")

        // 2. 정상 링크 실행 -> 책 상세 이동 -> 뒤로가기 시 홈 복귀
        // 잘못된 링크 검증은 자체 에러 다이얼로그였으므로 개발자 도구가 닫히지 않고 열려 있다.
        app.buttons["정상 링크"].tap()
        app.buttons["링크 실행"].tap()
        detail(app)
        back(app)
        home(app)
    }

    @MainActor func testMissingAndRetryLinksShowCommonErrorDialog() async throws {
        let app = try await prepared()
        app.launch()
        XCTAssertTrue(app.buttons["개발"].waitForExistence(timeout: 15))
        app.buttons["개발"].tap()
        XCTAssertTrue(app.staticTexts["책 상세 딥링크"].waitForExistence(timeout: 5))

        // 1. 없는 책 (404) -> "책을 찾을 수 없어요" 다이얼로그 -> 확인 후 홈 화면 복귀
        _ = try await URLSession.shared.data(from: base.appendingPathComponent("__692/mode/missing"))
        app.buttons["정상 링크"].tap()
        app.buttons["링크 실행"].tap()
        XCTAssertTrue(app.staticTexts["책을 찾을 수 없어요"].waitForExistence(timeout: 10))
        capture(app, "책 없음 오류")
        app.buttons["확인"].tap()
        home(app)

        // 2. 일시 장애 (503 -> 200 재시도) -> 다시 개발자 도구 열고 실행
        _ = try await URLSession.shared.data(from: base.appendingPathComponent("__692/mode/retry"))
        app.buttons["개발"].tap()
        app.buttons["정상 링크"].tap()
        app.buttons["링크 실행"].tap()
        XCTAssertTrue(app.buttons["다시 시도"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.staticTexts["다시 로그인해 주세요"].exists)
        app.buttons["다시 시도"].tap()
        detail(app)
        back(app)
        home(app)
    }
}
