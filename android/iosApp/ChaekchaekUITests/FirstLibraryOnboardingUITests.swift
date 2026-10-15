import XCTest

final class FirstLibraryOnboardingUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
    }

    @MainActor
    func testSkipCompletesInstallationOnboardingAndDoesNotShowAgain() throws {
        app.launchArguments = ["-uiTestingResetOnboarding"]
        app.launch()

        XCTAssertTrue(app.buttons["책 찾기"].waitForExistence(timeout: 5))
        let skip = app.buttons["건너뛰기"]
        XCTAssertTrue(skip.exists)
        try app.performAccessibilityAudit()

        skip.tap()
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))

        app.terminate()
        app.launchArguments = []
        app.launch()

        XCTAssertFalse(app.buttons["건너뛰기"].waitForExistence(timeout: 1))
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
    }
    @MainActor
    func testExistingInstallationSkipsOnboarding() throws {
        app.launchArguments = ["-uiTestingExistingInstallation"]
        app.launch()
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["책 찾기"].exists)
    }

    @MainActor
    func testSearchBackReturnsToIntroduction() throws {
        app.launchArguments = ["-uiTestingResetOnboarding"]
        app.launch()
        let start = app.buttons["책 찾기"]
        XCTAssertTrue(start.waitForExistence(timeout: 5))
        start.tap()
        let back = app.buttons["이전"]
        XCTAssertTrue(back.waitForExistence(timeout: 5), app.debugDescription)
        try app.performAccessibilityAudit()
        back.tap()
        XCTAssertTrue(start.waitForExistence(timeout: 5))
    }

}
