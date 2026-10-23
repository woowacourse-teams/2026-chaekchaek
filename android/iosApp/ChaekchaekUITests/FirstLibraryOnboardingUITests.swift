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


    @MainActor
    func testAnalyticsNavigationKeepsTabsWorking() throws {
        app.launchArguments = ["-uiTestingExistingInstallation"]
        app.launch()
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let cancelSignIn = springboard.alerts.buttons["취소"]
        if cancelSignIn.waitForExistence(timeout: 3) {
            cancelSignIn.tap()
        }
        for name in ["피드", "발견", "홈"] {
            let tab = app.buttons[name]
            XCTAssertTrue(tab.exists)
            tab.tap()
            XCTAssertTrue(tab.isSelected, app.debugDescription)
        }
        app.buttons["홈"].tap()
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    @MainActor
    func testAnalyticsLoginFailureAndCancellationKeepSignInSheet() throws {
        for flag in ["-uiTestingSignInFailure", "-uiTestingSignInCancellation"] {
            app.launchArguments = ["-uiTestingExistingInstallation", "-uiTestingGuest", flag]
            app.launch()
            let cancel = XCUIApplication(bundleIdentifier: "com.apple.springboard").alerts.buttons["취소"]
            if cancel.waitForExistence(timeout: 2) { cancel.tap() }
            let shelf = app.buttons["내 서재"]
            XCTAssertTrue(shelf.waitForExistence(timeout: 15), app.debugDescription)
            shelf.tap()
            let edit = app.buttons["편집"]
            XCTAssertTrue(edit.waitForExistence(timeout: 10))
            edit.tap()
            let google = app.buttons["GIDSignInButton"]
            XCTAssertTrue(google.waitForExistence(timeout: 5))
            google.tap()
            let failure = app.staticTexts["로그인하지 못했어요"]
            if flag == "-uiTestingSignInFailure" {
                XCTAssertTrue(failure.waitForExistence(timeout: 5))
                let attachment = XCTAttachment(screenshot: app.screenshot())
                attachment.lifetime = .keepAlways
                add(attachment)
                app.buttons["닫기"].firstMatch.tap()
                XCTAssertFalse(failure.exists)
            } else {
                XCTAssertFalse(failure.waitForExistence(timeout: 2))
            }
            XCTAssertTrue(app.staticTexts["로그인이 필요해요"].exists)
            XCTAssertTrue(google.exists)
            app.terminate()
        }
    }
}
