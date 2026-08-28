import XCTest

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

        XCTAssertTrue(app.staticTexts["로그인이 필요해요"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Apple로 계속하기"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["GIDSignInButton"].waitForExistence(timeout: 5))

        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "iOS 네이티브 로그인 버튼"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
