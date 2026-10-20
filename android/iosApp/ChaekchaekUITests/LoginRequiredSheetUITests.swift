import XCTest

final class LoginRequiredSheetUITests: XCTestCase {
    // CHAEKCHAEK_API_ENVIRONMENT=local 빌드와 실패 후 성공하는 로컬 fixture가 필요하다.
    @MainActor
    func testReviewDraftSurvivesFailureAndClosesAfterRetry() async throws {
        let base = URL(string: "http://127.0.0.1:9090")!
        guard let (data, _) = try? await URLSession.shared.data(from: base.appendingPathComponent("__review-draft-qa")),
              String(data: data, encoding: .utf8)?.contains("review-draft") == true else {
            throw XCTSkip("감상 보존 전용 로컬 fixture가 실행 중일 때만 검증한다")
        }
        var reset = URLRequest(url: base.appendingPathComponent("__review-draft-qa/reset"))
        reset.httpMethod = "POST"
        _ = try await URLSession.shared.data(for: reset)

        let app = XCUIApplication()
        app.launchArguments += ["-uiTestingGuest", "-uiTestingExistingInstallation"]
        app.launch()
        let book = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "감상 보존 검증")).firstMatch
        guard book.waitForExistence(timeout: 15) else {
            XCTFail("로컬 fixture 도서가 표시되지 않아 검증을 중단한다")
            return
        }
        book.tap()
        let open = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "이 순간의 감상 남기기")).firstMatch
        XCTAssertTrue(open.waitForExistence(timeout: 10))
        open.tap()
        let content = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 (필수)")).firstMatch
        guard content.waitForExistence(timeout: 10) else {
            XCTFail("감상 입력란 접근 실패: \(app.debugDescription)")
            return
        }
        XCTAssertFalse(app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "목차 또는 챕터")).firstMatch.exists)
        XCTAssertFalse(app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "쪽수")).firstMatch.exists)
        XCTAssertFalse(app.sliders["별점 선택"].exists)
        let quote = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "인상 깊은 문구")).firstMatch
        revealReviewControl(quote, in: app, scrollingUp: false)
        quote.tap()
        quote.typeText("saved quote")
        revealReviewControl(content, in: app, scrollingUp: false)
        content.tap()
        content.typeText("draft must survive")
        let spoiler = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "스포일러")).firstMatch
        revealReviewControl(spoiler, in: app, scrollingUp: true)
        spoiler.tap()
        let save = app.buttons["감상 남기기"]
        revealReviewControl(save, in: app, scrollingUp: true)
        try await Task.sleep(for: .milliseconds(500))
        save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).press(forDuration: 0.15)
        // 첫 요청은 500으로 실패한다. 응답 뒤에도 같은 작성창과 입력이 남아야 한다.
        try await Task.sleep(for: .seconds(2))
        let (attemptData, _) = try await URLSession.shared.data(from: base.appendingPathComponent("__review-draft-qa/state"))
        let attempts = try XCTUnwrap(JSONSerialization.jsonObject(with: attemptData) as? [String: [[String: Any]]])
        guard attempts["requests"]?.count == 1 else {
            attachScreenshot(named: "감상 저장 버튼 입력 확인")
            XCTFail("첫 저장 요청이 전달되지 않았다: \(save.debugDescription)")
            return
        }
        guard content.exists else {
            attachScreenshot(named: "수정 전 저장 실패로 작성창이 닫힘")
            XCTFail("저장 실패 뒤 작성창과 입력이 사라졌다")
            return
        }
        revealReviewControl(content, in: app, scrollingUp: false)
        XCTAssertEqual(content.value as? String, "draft must survive")
        XCTAssertEqual(quote.value as? String, "saved quote")
        attachScreenshot(named: "감상 저장 실패 후 입력 보존")
        revealReviewControl(save, in: app, scrollingUp: true)
        try await Task.sleep(for: .milliseconds(500))
        save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).press(forDuration: 0.15)
        let closed = NSPredicate(format: "exists == false")
        let closedExpectation = expectation(for: closed, evaluatedWith: content)
        await fulfillment(of: [closedExpectation], timeout: 10)
        let (savedData, _) = try await URLSession.shared.data(from: base.appendingPathComponent("__review-draft-qa/state"))
        let saved = try XCTUnwrap(JSONSerialization.jsonObject(with: savedData) as? [String: [[String: Any]]])
        XCTAssertEqual(saved["requests"]?.count, 2)
        let submissions = try XCTUnwrap(saved["requests"])
        XCTAssertEqual(submissions.first?["content"] as? String, "draft must survive")
        XCTAssertEqual(submissions.last?["quote"] as? String, "saved quote")
        XCTAssertNil(submissions.last?["chapter"] as? String)
        XCTAssertNil(submissions.last?["currentPage"] as? Int)
        XCTAssertNil(submissions.last?["rating"] as? Double)
        XCTAssertEqual(submissions.last?["isSpoiler"] as? Bool, true)
        let first = try XCTUnwrap(submissions.first)
        let last = try XCTUnwrap(submissions.last)
        XCTAssertTrue(NSDictionary(dictionary: first).isEqual(to: last))
        attachScreenshot(named: "재시도 성공 후 작성창 닫힘")
        open.tap()
        XCTAssertTrue(content.waitForExistence(timeout: 5))
        XCTAssertTrue((content.value as? String ?? "").isEmpty)
        attachScreenshot(named: "새 감상 작성창 초기화")
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion])
    }

    @MainActor
    private func revealReviewControl(_ control: XCUIElement, in app: XCUIApplication, scrollingUp: Bool) {
        for _ in 0..<5 {
            let keyboard = app.keyboards.firstMatch
            let bottom = keyboard.exists ? keyboard.frame.minY - 100 : app.frame.maxY - 34
            if control.exists && control.isHittable && control.frame.height >= 40 && control.frame.maxY <= bottom {
                return
            }
            // 텍스트 입력란과 키보드가 화면 스와이프를 소비하므로 시트의 빈 오른쪽 여백을 이용한다.
            let top = max(160, bottom - 300)
            let origin = app.coordinate(withNormalizedOffset: .zero)
            let start = origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: scrollingUp ? bottom - 24 : top))
            let end = origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: scrollingUp ? top : bottom - 24))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
    }

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
    func testHomeQuickSaveKeepsPositionAfterLoginCancellation() throws {
        try verifyQuickSaveLoginCancellation(tab: nil)
    }

    @MainActor
    func testFeedQuickSaveKeepsPositionAfterLoginCancellation() throws {
        try verifyQuickSaveLoginCancellation(tab: "피드")
    }

    @MainActor
    private func verifyQuickSaveLoginCancellation(tab: String?) throws {
        let app = XCUIApplication()
        app.launchArguments.append("-uiTestingGuest")
        app.launch()
        XCTAssertTrue(app.buttons["내 서재"].waitForExistence(timeout: 15))
        if let tab {
            app.buttons[tab].tap()
        }
        let save = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "서재에 담기")).firstMatch
        XCTAssertTrue(save.waitForExistence(timeout: 20))
        for _ in 0..<5 {
            if save.isHittable { break }
            app.swipeUp()
        }
        XCTAssertTrue(save.isHittable)
        XCTAssertGreaterThanOrEqual(save.frame.height, 44)
        XCTAssertGreaterThanOrEqual(save.frame.width, 44)
        let originalY = save.frame.minY
        save.tap()
        XCTAssertTrue(app.staticTexts["로그인이 필요해요"].waitForExistence(timeout: 5))
        app.buttons["닫기"].firstMatch.tap()
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        XCTAssertEqual(save.frame.minY, originalY, accuracy: 3)
        XCTAssertFalse(app.staticTexts["로그인이 필요해요"].exists)
        attachScreenshot(named: "\(tab ?? "홈") 바로 담기 로그인 취소")
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion]) { issue in
            // 기존 캐러셀 표시점의 작은 터치 영역만 기록 후 제외한다.
            // 홈 전체 접근성 감사 실패와 재현 근거는 library-save-interaction-plan.md에 남긴다.
            guard tab == nil,
                  issue.compactDescription == "Hit area is too small",
                  let label = issue.element?.label,
                  label.range(of: "^[1-9][0-9]*번째 인기 책 보기$", options: .regularExpression) != nil else {
                return false
            }
            let attachment = XCTAttachment(string: "\(label): \(issue.detailedDescription)")
            attachment.name = "기존 홈 캐러셀 터치 영역 미통과"
            attachment.lifetime = .keepAlways
            self.add(attachment)
            return true
        }
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
