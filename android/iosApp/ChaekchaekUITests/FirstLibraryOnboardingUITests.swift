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
}
