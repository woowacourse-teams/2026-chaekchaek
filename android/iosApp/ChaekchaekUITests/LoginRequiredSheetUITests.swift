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
