import XCTest

final class LoginRequiredSheetUITests: XCTestCase {
    // scripts/reply-compatibility-fixture.py와 local API 환경에서 실행한다.
    @MainActor
    func testReplyResponseWithoutDeletedRoundTripAndReentry() async throws {
        let base = URL(string: "http://127.0.0.1:19063")!
        var probe = URLRequest(url: base.appendingPathComponent("__reply-compat"))
        probe.timeoutInterval = 2
        guard let (marker, _) = try? await URLSession.shared.data(for: probe),
              String(data: marker, encoding: .utf8)?.contains("reply-compat") == true else {
            throw XCTSkip("reply-compat fixture를 실행한 local 빌드에서 검증한다")
        }
        var reset = URLRequest(url: base.appendingPathComponent("__reply-compat/reset"))
        reset.httpMethod = "POST"
        _ = try await URLSession.shared.data(for: reset)
        let app = XCUIApplication()
        app.launchArguments.append("-uiTestingGuest")
        app.launch()
        func press(_ element: XCUIElement) async throws {
            try await Task.sleep(for: .milliseconds(700))
            element.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).press(forDuration: 0.15)
        }
        let book = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "Reply compatibility")).firstMatch
        XCTAssertTrue(book.waitForExistence(timeout: 20))
        try await press(book)
        let review = app.staticTexts["Compatibility review"]
        XCTAssertTrue(review.waitForExistence(timeout: 15))
        func reveal(_ element: XCUIElement) {
            for _ in 0..<6 {
                if element.exists {
                    let frame = element.frame
                    if element.isHittable && frame.minY > app.frame.minY + 100 && frame.maxY < app.frame.maxY - 100 { return }
                    if frame.minY < app.frame.minY + 100 { app.swipeDown() }
                    else { app.swipeUp() }
                } else { app.swipeUp() }
            }
        }
        reveal(review)
        XCTAssertTrue(app.staticTexts["삭제된 감상입니다."].exists)
        XCTAssertTrue(app.staticTexts["Retained reply"].exists)
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "감상 목록 개수 2")).firstMatch.exists)
        let existing = app.staticTexts["Existing reply"]
        reveal(existing)
        XCTAssertTrue(existing.exists)
        let all = app.buttons["답글 2개 모두 보기"]
        reveal(all)
        try await press(all)
        let second = app.staticTexts["Second page reply"]
        guard second.waitForExistence(timeout: 10) else {
            attachScreenshot(named: "563 reply pagination failed")
            XCTFail("답글 전체 보기 후 다음 페이지가 표시되지 않았다: \(app.debugDescription)")
            return
        }
        reveal(second)
        attachScreenshot(named: "563 reply pages")
        let open = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "감상에 답글 작성")).firstMatch
        if !open.isHittable { app.swipeDown() }
        reveal(open)
        try await press(open)
        let input = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "답글")).firstMatch
        XCTAssertTrue(input.waitForExistence(timeout: 5))
        input.tap()
        input.typeText("Created reply")
        let submit = app.buttons["등록"]
        revealReviewControl(submit, in: app, scrollingUp: true)
        let created = app.staticTexts["Created reply"]
        for _ in 0..<3 {
            try await Task.sleep(for: .milliseconds(700))
            if created.exists { break }
            if submit.exists && submit.isEnabled { try await press(submit) }
            if created.waitForExistence(timeout: 3) { break }
        }
        guard created.waitForExistence(timeout: 10) else {
            attachScreenshot(named: "563 reply create failed")
            XCTFail("답글 등록 후 결과가 표시되지 않았다: \(app.debugDescription)")
            return
        }
        reveal(created)
        let manage = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "내 답글 수정 또는 삭제")).firstMatch
        reveal(manage)
        try await press(manage)
        try await press(app.buttons["수정"])
        XCTAssertTrue(input.waitForExistence(timeout: 5))
        // 확대 글자에서는 가운데 탭 후 고정 횟수 삭제가 원문 한 글자를 남길 수 있다.
        // 입력 끝에 커서를 놓고 실제 값이 비었는지 확인한 뒤 새 내용을 입력한다.
        input.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
        for _ in 0..<3 {
            let current = input.value as? String ?? ""
            if current.isEmpty { break }
            input.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: current.count + 1))
            try await Task.sleep(for: .milliseconds(300))
        }
        XCTAssertEqual(input.value as? String ?? "", "", "수정 입력의 기존 내용을 모두 지운다")
        input.typeText("Updated reply")
        let save = app.buttons["수정 저장"]
        revealReviewControl(save, in: app, scrollingUp: true)
        let updated = app.staticTexts["Updated reply"]
        for _ in 0..<3 {
            try await Task.sleep(for: .milliseconds(700))
            if updated.exists { break }
            if save.exists && save.isEnabled { try await press(save) }
            if updated.waitForExistence(timeout: 3) { break }
        }
        guard updated.waitForExistence(timeout: 10) else {
            attachScreenshot(named: "563 reply update failed")
            XCTFail("답글 수정 후 결과가 표시되지 않았다: \(app.debugDescription)")
            return
        }
        reveal(updated)
        attachScreenshot(named: "563 reply updated")
        reveal(manage)
        try await press(manage)
        try await press(app.buttons["삭제"])
        XCTAssertTrue(app.staticTexts["답글을 삭제할까요?"].waitForExistence(timeout: 5))
        try await press(app.buttons["삭제"])
        let removed = expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: updated)
        await fulfillment(of: [removed], timeout: 10)
        let back = app.buttons["뒤로 가기"]
        for _ in 0..<8 {
            if back.exists && back.isHittable && back.frame.minY > app.frame.minY + 65 { break }
            app.swipeDown()
        }
        // 스크롤 관성이 남아 있으면 첫 입력이 이동을 멈추는 데 소비될 수 있다.
        for _ in 0..<3 {
            try await Task.sleep(for: .milliseconds(700))
            if !back.exists { break }
            try await press(back)
        }
        let returnedHome = expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: back)
        await fulfillment(of: [returnedHome], timeout: 10)
        XCTAssertTrue(book.waitForExistence(timeout: 10))
        try await press(book)
        XCTAssertTrue(review.waitForExistence(timeout: 10))
        reveal(existing)
        XCTAssertTrue(existing.exists)
        XCTAssertFalse(updated.exists)
        XCTAssertTrue(app.staticTexts["삭제된 감상입니다."].exists)
        XCTAssertTrue(app.staticTexts["Retained reply"].exists)
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "감상 목록 개수 2")).firstMatch.exists)
        attachScreenshot(named: "563 reply reentry")
        let reviewManage = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "내 감상 수정 또는 삭제")).firstMatch
        reveal(reviewManage)
        try await press(reviewManage)
        try await press(app.buttons["삭제"])
        XCTAssertTrue(app.staticTexts["감상을 삭제할까요?"].waitForExistence(timeout: 5))
        try await press(app.buttons["삭제"])
        let reviewRemoved = expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: review)
        await fulfillment(of: [reviewRemoved], timeout: 10)
        XCTAssertEqual(app.staticTexts.matching(identifier: "삭제된 감상입니다.").count, 2)
        reveal(existing)
        XCTAssertTrue(existing.exists)
        XCTAssertFalse(reviewManage.exists)
        XCTAssertFalse(open.exists)
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "감상 목록 개수 2")).firstMatch.exists)
        attachScreenshot(named: "563 deleted review retained")
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped])
        let (data, _) = try await URLSession.shared.data(from: base.appendingPathComponent("__reply-compat/state"))
        let state = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        let requests = try XCTUnwrap(state["requests"] as? [[String: Any]])
        XCTAssertEqual(requests.filter {
            $0["method"] as? String == "POST" && $0["path"] as? String == "/api/v1/reviews/7/replies"
        }.count, 1)
        XCTAssertEqual(requests.filter { $0["method"] as? String == "PATCH" }.count, 1)
        XCTAssertEqual(requests.filter { $0["method"] as? String == "DELETE" }.count, 2)
        let deletionIndex = try XCTUnwrap(requests.firstIndex { $0["method"] as? String == "DELETE" })
        XCTAssertTrue(requests.dropFirst(deletionIndex + 1).contains {
            $0["path"] as? String == "/api/v1/books/by-isbn/9780000000042"
        })
        XCTAssertTrue(requests.contains { ($0["path"] as? String)?.contains("page=2") == true })
        XCTAssertEqual((state["replies"] as? [[String: Any]])?.count, 2)
        XCTAssertEqual(state["review_deleted"] as? Bool, true)
    }


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
        app.launchArguments.append("-uiTestingGuest")
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
        // 아래 필드부터 채워 키보드가 펼쳐질 때 입력란을 가리는 영향을 줄인다.
        let chapter = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "목차 또는 챕터")).firstMatch
        revealReviewControl(chapter, in: app, scrollingUp: true)
        chapter.tap()
        chapter.typeText("chapter one")
        let quote = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "인상 깊은 문구")).firstMatch
        revealReviewControl(quote, in: app, scrollingUp: false)
        quote.tap()
        quote.typeText("saved quote")
        revealReviewControl(content, in: app, scrollingUp: false)
        content.tap()
        content.typeText("draft must survive")
        let spoiler = app.buttons["스포일러"]
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
        let retry = app.buttons["다시 시도"]
        XCTAssertTrue(retry.waitForExistence(timeout: 5))
        let close = app.buttons["닫기"].firstMatch
        XCTAssertEqual(close.frame.midY, retry.frame.midY, accuracy: 1)
        XCTAssertLessThan(close.frame.maxX, retry.frame.minX)
        try await Task.sleep(for: .milliseconds(500))
        attachScreenshot(named: "공통 오류 다이얼로그 가로 버튼")
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion])
        close.tap()
        guard content.exists else {
            attachScreenshot(named: "수정 전 저장 실패로 작성창이 닫힘")
            XCTFail("저장 실패 뒤 작성창과 입력이 사라졌다")
            return
        }
        revealReviewControl(content, in: app, scrollingUp: false)
        XCTAssertEqual(content.value as? String, "draft must survive")
        XCTAssertEqual(quote.value as? String, "saved quote")
        XCTAssertEqual(chapter.value as? String, "chapter one")
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
        XCTAssertEqual(submissions.last?["chapter"] as? String, "chapter one")
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

    @MainActor
    func testLoginFailureClosesOnlyErrorAndCancellationShowsNoError() throws {
        for flag in ["-uiTestingSignInFailure", "-uiTestingSignInCancellation"] {
            let app = XCUIApplication()
            app.launchArguments += ["-uiTestingGuest", flag]
            app.launch()
            let shelf = app.buttons["내 서재"]
            XCTAssertTrue(shelf.waitForExistence(timeout: 15))
            shelf.tap()
            let edit = app.buttons["편집"]
            XCTAssertTrue(edit.waitForExistence(timeout: 10))
            edit.tap()
            let google = app.buttons["GIDSignInButton"]
            XCTAssertTrue(google.waitForExistence(timeout: 5))
            google.tap()
            if flag == "-uiTestingSignInFailure" {
                let title = app.staticTexts["로그인하지 못했어요"]
                XCTAssertTrue(title.waitForExistence(timeout: 5))
                let retry = app.buttons["다시 시도"]
                XCTAssertTrue(retry.exists)
                attachScreenshot(named: "로그인 시트 위 공통 오류")
                try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion])
                app.buttons["닫기"].firstMatch.tap()
                XCTAssertFalse(title.exists)
                XCTAssertTrue(app.staticTexts["로그인이 필요해요"].exists)
                XCTAssertTrue(google.exists)
            } else {
                XCTAssertFalse(app.staticTexts["로그인하지 못했어요"].exists)
                XCTAssertTrue(app.staticTexts["로그인이 필요해요"].exists)
            }
            app.terminate()
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
