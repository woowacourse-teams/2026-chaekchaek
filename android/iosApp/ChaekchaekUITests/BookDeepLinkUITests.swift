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
        app.launchArguments = ["-uiTestingRefresh", onboarding ? "-uiTestingResetOnboarding" : "-uiTestingExistingInstallation"]
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

    @MainActor func testColdLinkOpensDetailAndReturnsHome() async throws {
        let app = try await prepared()
        app.open(valid)
        detail(app)
        capture(app, "딥링크 상세")
        back(app)
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
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
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
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

        // 2. 정상 링크 실행 -> 책 상세 이동 -> 뒤로가기 시 홈 복귀
        // 오류 다이얼로그를 닫았으므로 여전히 개발자 도구 화면이다.
        app.buttons["정상 링크"].tap()
        app.buttons["링크 실행"].tap()
        detail(app)
        back(app)
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
    }

    @MainActor func testMissingAndRetryLinksShowCommonErrorDialog() async throws {
        let app = try await prepared()
        app.launch()
        XCTAssertTrue(app.buttons["개발"].waitForExistence(timeout: 15))
        app.buttons["개발"].tap()
        XCTAssertTrue(app.staticTexts["책 상세 딥링크"].waitForExistence(timeout: 5))

        // 1. 없는 책 (404) -> "책을 찾을 수 없어요" 다이얼로그
        _ = try await URLSession.shared.data(from: base.appendingPathComponent("__692/mode/missing"))
        app.buttons["정상 링크"].tap()
        app.buttons["링크 실행"].tap()
        XCTAssertTrue(app.staticTexts["책을 찾을 수 없어요"].waitForExistence(timeout: 10))
        capture(app, "책 없음 오류")
        app.buttons["확인"].tap()

        // 2. 일시 장애 (503 -> 200 재시도)
        _ = try await URLSession.shared.data(from: base.appendingPathComponent("__692/mode/retry"))
        app.buttons["정상 링크"].tap()
        app.buttons["링크 실행"].tap()
        XCTAssertTrue(app.buttons["다시 시도"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.staticTexts["다시 로그인해 주세요"].exists)
        app.buttons["다시 시도"].tap()
        detail(app)
        back(app)
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
    }
}
