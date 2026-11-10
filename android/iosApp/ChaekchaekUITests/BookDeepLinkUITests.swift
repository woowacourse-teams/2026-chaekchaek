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
        // Simulator 전용 fixture 실행. 실제 사용자 기기에서는 이 옵션을 사용하지 않는다.
        app.launchArguments = ["-uiTestingRefresh", onboarding ? "-uiTestingResetOnboarding" : "-uiTestingExistingInstallation"]
        app.launchEnvironment["CHAEKCHAEK_UI_TEST_API_URL"] = base.absoluteString
        return app
    }

    @MainActor private func detail(_ app: XCUIApplication) {
        XCTAssertTrue(app.staticTexts["딥링크 검증 도서"].waitForExistence(timeout: 15), app.debugDescription)
    }
    @MainActor private func back(_ app: XCUIApplication) {
        let button = app.buttons["뒤로 가기"]
        if !button.isHittable {
            let top = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "TOP")).firstMatch
            if top.exists { top.tap() }
        }
        XCTAssertTrue(button.waitForExistence(timeout: 5)); button.tap()
    }
    @MainActor private func capture(_ app: XCUIApplication, _ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name; attachment.lifetime = .keepAlways; add(attachment)
    }

    @MainActor func testColdLinkReturnsHomeAndBackgroundLinkReturnsDiscover() async throws {
        let app = try await prepared()
        app.open(valid); detail(app); capture(app, "딥링크 상세")
        back(app); XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
        app.buttons["발견"].tap()
        XCUIDevice.shared.press(.home)
        app.open(valid); detail(app); back(app)
        XCTAssertTrue(app.buttons["발견"].isSelected)
    }

    @MainActor func testSameBookKeepsSortAndInvalidOrMissingLinkKeepsCurrentScreen() async throws {
        let app = try await prepared(); app.open(valid); detail(app)
        let sort = app.buttons["인기순"]
        if !sort.isHittable { app.swipeUp() }
        XCTAssertTrue(sort.waitForExistence(timeout: 5)); sort.tap(); app.buttons["최신순"].tap()
        app.open(valid)
        XCTAssertTrue(app.buttons["최신순"].waitForExistence(timeout: 5))
        app.open(URL(string: "chaekchaek://books/not-a-book")!)
        XCTAssertTrue(app.staticTexts["링크를 열 수 없어요"].waitForExistence(timeout: 5))
        app.buttons["확인"].tap()
        XCTAssertTrue(app.buttons["최신순"].exists)
        app.open(URL(string: "chaekchaek://books/9791172376215")!)
        XCTAssertTrue(app.staticTexts["책을 찾을 수 없어요"].waitForExistence(timeout: 10))
        capture(app, "책 없음 오류"); app.buttons["확인"].tap()
        XCTAssertTrue(app.buttons["최신순"].exists)
        back(app); XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
    }

    @MainActor func testOnboardingRetainsTargetAndRetryDoesNotRequireLogin() async throws {
        let app = try await prepared(onboarding: true)
        app.open(valid)
        XCTAssertTrue(app.buttons["건너뛰기"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.staticTexts["딥링크 검증 도서"].exists)
        app.buttons["건너뛰기"].tap(); detail(app); back(app)
        app.open(URL(string: "chaekchaek://books/9788936433598")!)
        XCTAssertTrue(app.buttons["다시 시도"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.staticTexts["다시 로그인해 주세요"].exists)
        app.buttons["다시 시도"].tap(); detail(app)
    }

    @MainActor func testDeveloperToolUsesRealURLAndPreservesInputWhenRejected() async throws {
        let app = try await prepared(); app.launch()
        XCTAssertTrue(app.buttons["개발"].waitForExistence(timeout: 15)); app.buttons["개발"].tap()
        XCTAssertTrue(app.staticTexts["책 상세 딥링크"].waitForExistence(timeout: 5))
        capture(app, "개발자 도구 딥링크")
        app.buttons["잘못된 링크"].tap(); app.buttons["링크 실행"].tap()
        XCTAssertTrue(app.staticTexts["링크를 열 수 없어요"].waitForExistence(timeout: 5)); app.buttons["확인"].tap()
        app.buttons["정상 링크"].tap(); app.buttons["링크 실행"].tap(); detail(app)
        back(app); XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
    }
}
