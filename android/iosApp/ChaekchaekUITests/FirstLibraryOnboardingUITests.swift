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
    func testSearchKeyboardDismissalPreservesOnboardingQuery() throws {
        app.launchArguments = ["-uiTestingResetOnboarding"]
        app.launch()
        let start = app.buttons["책 찾기"]
        XCTAssertTrue(start.waitForExistence(timeout: 5))
        start.tap()

        let guidance = app.staticTexts["책 제목이나 작가로 찾아보세요."]
        XCTAssertTrue(guidance.waitForExistence(timeout: 5), app.debugDescription)
        // 기존 닉네임 테스트처럼 textField와 textView 노출을 모두 조회한다.
        let field = app.textFields.firstMatch.waitForExistence(timeout: 2)
            ? app.textFields.firstMatch : app.textViews.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 5), app.debugDescription)
        field.tap()
        let keyboard = app.keyboards.firstMatch
        XCTAssertTrue(keyboard.waitForExistence(timeout: 5))
        let query = "keyboard"
        field.typeText(query)
        XCTAssertEqual(field.value as? String, query)

        // 입력란 재진입이 빈 영역 탭으로 처리되지 않는지 반복 확인한다.
        for _ in 0..<3 {
            XCTAssertGreaterThan(field.frame.minY, guidance.frame.maxY)
            let blankY = (guidance.frame.maxY + field.frame.minY) / 2
            app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(
                dx: field.frame.midX - app.frame.minX,
                dy: blankY - app.frame.minY
            )).tap()
            XCTAssertTrue(keyboard.waitForNonExistence(timeout: 5), "빈 영역을 누르면 키보드가 닫혀야 합니다")
            XCTAssertEqual(field.value as? String, query)

            field.tap()
            XCTAssertTrue(keyboard.waitForExistence(timeout: 5))
            XCTAssertEqual(field.value as? String, query)
        }
        // IME 검색 동작으로 제출한다. API 결과는 검사하지 않는다.
        field.typeText("\n")
        XCTAssertTrue(keyboard.waitForNonExistence(timeout: 5), "검색을 실행하면 키보드가 닫혀야 합니다")
        XCTAssertEqual(field.value as? String, query)
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "검색 후 키보드 닫힘과 검색어 보존"
        attachment.lifetime = .keepAlways
        add(attachment)
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
        for name in ["피드", "검색", "홈"] {
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

final class RemotePolicyUITests: XCTestCase {
    @MainActor
    func testPublishedPolicyIsReceivedWithoutStoreRelease() throws {
        guard ProcessInfo.processInfo.environment["VERIFY_LIVE_REMOTE_POLICY"] == "1" else {
            throw XCTSkip("Firebase 게시 후 실행하는 실제 수신 검증")
        }
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestingExistingInstallation"]
        app.launch()
        XCTAssertTrue(app.buttons["개발"].waitForExistence(timeout: 15))
        app.buttons["개발"].tap()
        let refresh = app.buttons["설정 다시 받기"]
        XCTAssertTrue(refresh.waitForExistence(timeout: 10))
        refresh.tap()
        let received = app.staticTexts.matching(NSPredicate(
            format: "label CONTAINS %@ AND label CONTAINS %@ AND NOT (label CONTAINS %@)",
            "설정 출처: 원격", "검증 문구: 원격 설정 연결 확인", "마지막 수신: 없음"
        )).firstMatch
        XCTAssertTrue(received.waitForExistence(timeout: 30))
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "Firebase 실제 정책 수신"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    @MainActor
    func testRequiredUpdateHasNoDismissAndKeepsAppBlocked() {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestingExistingInstallation", "-uiTestingRequiredUpdate"]
        app.launch()
        XCTAssertTrue(app.staticTexts["업데이트가 필요해요"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["업데이트"].exists)
        XCTAssertFalse(app.buttons["나중에"].exists)
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "강제 업데이트"
        attachment.lifetime = .keepAlways
        add(attachment)
        XCTAssertTrue(app.staticTexts["업데이트가 필요해요"].exists)
    }

    @MainActor
    func testRecommendedUpdateCanBeDeferredWithoutLosingHome() {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestingExistingInstallation", "-uiTestingRecommendedUpdate"]
        app.launch()
        XCTAssertTrue(app.staticTexts["새 버전이 있어요"].waitForExistence(timeout: 10))
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "권장 업데이트"
        attachment.lifetime = .keepAlways
        add(attachment)
        app.buttons["나중에"].tap()
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["새 버전이 있어요"].exists)
    }
}
