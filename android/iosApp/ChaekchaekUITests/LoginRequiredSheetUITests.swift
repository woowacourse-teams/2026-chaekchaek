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
    func testHomePullRefreshAndListEndKeepNavigationVisible() throws {
        let app = XCUIApplication()
        app.launchArguments.append("-uiTestingGuest")
        app.launch()
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 15))
        let end = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "여기까지 모두 봤어요")).firstMatch
        for _ in 0..<8 {
            if end.exists && end.isHittable { break }
            app.swipeUp()
        }
        XCTAssertTrue(end.waitForExistence(timeout: 10))
        attachScreenshot(named: "홈 목록 끝")
        app.buttons["홈"].tap()
        app.swipeDown()
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertTrue(app.buttons["내 프로필"].isHittable)
        XCTAssertTrue(app.buttons["발견"].exists)
        XCTAssertTrue(app.buttons["내 서재"].exists)
        attachScreenshot(named: "홈 당김 후 기존 화면")
        // hitRegion은 시스템 홈 인디케이터에서도 실패해 별도 검증 기록으로 남긴다.
        try app.performAccessibilityAudit(for: [.textClipped])
    }

    @MainActor
    func testEmptySearchPullKeepsSubmittedInput() throws {
        let app = XCUIApplication()
        app.launchArguments.append("-uiTestingGuest")
        app.launch()
        XCTAssertTrue(app.buttons["발견"].waitForExistence(timeout: 15))
        app.buttons["발견"].tap()
        let field = app.textViews["search-query-field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText("zzxqrefresh529nomatch\n")
        let empty = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "검색 결과가 없어요")).firstMatch
        XCTAssertTrue(empty.waitForExistence(timeout: 15))
        app.swipeDown()
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertTrue(empty.exists)
        XCTAssertEqual(field.value as? String, "zzxqrefresh529nomatch")
        attachScreenshot(named: "빈 검색 결과 당김 후 검색어 유지")
        // hitRegion은 시스템 홈 인디케이터에서도 실패해 별도 검증 기록으로 남긴다.
        try app.performAccessibilityAudit(for: [.textClipped])
    }

    @MainActor
    private func waitForRefreshIndicatorToClose(in app: XCUIApplication) {
        let indicator = app.staticTexts.matching(NSPredicate(
            format: "label CONTAINS %@ OR label CONTAINS %@ OR label CONTAINS %@ OR label CONTAINS %@",
            "당겨서 새로고침", "놓으면 새로고침", "갱신 요청 중", "새로고침 중이에요"
        )).firstMatch
        expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: indicator)
        waitForExpectations(timeout: 10)
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
