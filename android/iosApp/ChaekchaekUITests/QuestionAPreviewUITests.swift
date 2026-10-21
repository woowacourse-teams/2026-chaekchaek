import XCTest

final class QuestionAPreviewUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testReferenceTabsAndQuestionAbsence() throws {
        let app = launchPreview()
        let questionHeading = element("여운 한 줄", in: app)
        reveal(questionHeading, in: app)
        XCTAssertTrue(questionHeading.exists)

        let reference = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "줄거리와 발췌 보기")).firstMatch
        reveal(reference, in: app)
        let questionY = questionHeading.frame.minY
        reference.tap()
        let synopsis = element("줄거리", in: app)
        let excerpt = element("본문 발췌", in: app)
        XCTAssertTrue(synopsis.waitForExistence(timeout: 5))
        XCTAssertTrue(excerpt.exists)
        excerpt.tap()
        attachScreenshot(named: "질문 자료 본문 발췌 탭")
        synopsis.tap()
        let close = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "질문과 장면 닫기")).firstMatch
        close.tap()
        XCTAssertFalse(close.exists)
        XCTAssertEqual(questionHeading.frame.minY, questionY, accuracy: 2)

        let noQuestion = element("질문 없는 책", in: app)
        reveal(noQuestion, in: app, scrollingUp: false)
        noQuestion.tap()
        XCTAssertFalse(questionHeading.exists)
        XCTAssertFalse(reference.exists)
        attachScreenshot(named: "질문 없는 프리뷰 책")
    }

    @MainActor
    func testComposerDistinguishesUnselectedRatingAndActualZero() throws {
        let app = launchPreview()
        let compose = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 쓰기")).firstMatch
        reveal(compose, in: app)
        compose.tap()

        let content = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 (필수)")).firstMatch
        XCTAssertTrue(content.waitForExistence(timeout: 5))
        content.tap()
        content.typeText("Question A zero draft")
        let rating = app.otherElements.matching(NSPredicate(format: "label == %@ AND value != nil", "별점 (선택)")).firstMatch
        reveal(rating, in: app)
        XCTAssertTrue(rating.exists)
        XCTAssertTrue((rating.value as? String)?.contains("미선택") == true, rating.debugDescription)
        // iOS Compose는 별점 조절 컨트롤을 값이 있는 Other로 노출한다. 실제 0점 터치 영역을 누른다.
        rating.coordinate(withNormalizedOffset: CGVector(dx: 0.02, dy: 0.5)).tap()
        XCTAssertTrue((rating.value as? String)?.contains("0.0점") == true, rating.debugDescription)
        XCTAssertFalse(app.textViews.matching(NSPredicate(format: "label CONTAINS %@", "쪽수")).firstMatch.exists)
        XCTAssertFalse(app.textViews.matching(NSPredicate(format: "label CONTAINS %@", "챕터")).firstMatch.exists)
        attachScreenshot(named: "선택한 실제 0점")

        let close = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "닫기")).firstMatch
        reveal(close, in: app, scrollingUp: false)
        close.tap()
        let keep = element("계속 작성", in: app)
        XCTAssertTrue(keep.waitForExistence(timeout: 5))
        keep.tap()
        reveal(content, in: app, scrollingUp: false)
        XCTAssertEqual(content.value as? String, "Question A zero draft")
        reveal(rating, in: app)
        XCTAssertTrue((rating.value as? String)?.contains("0.0점") == true, rating.debugDescription)
        attachScreenshot(named: "0점과 본문 초안 보존")
    }

    @MainActor
    func testUnselectedRatingCanBeSaved() throws {
        let app = launchPreview()
        let compose = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 쓰기")).firstMatch
        reveal(compose, in: app)
        compose.tap()
        let content = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 (필수)")).firstMatch
        XCTAssertTrue(content.waitForExistence(timeout: 5))
        content.tap()
        content.typeText("Question A unselected draft")
        let rating = app.otherElements.matching(NSPredicate(format: "label == %@ AND value != nil", "별점 (선택)")).firstMatch
        reveal(rating, in: app)
        XCTAssertTrue((rating.value as? String)?.contains("미선택") == true, rating.debugDescription)
        let submit = app.buttons.matching(NSPredicate(format: "label == %@", "감상 남기기")).firstMatch
        reveal(submit, in: app)
        submit.tap()
        let saved = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", "로컬 프리뷰 감상: Question A unselected draft", "미선택")).firstMatch
        XCTAssertTrue(saved.waitForExistence(timeout: 5), app.debugDescription)
        attachScreenshot(named: "별점 미선택 감상 저장")
    }

    @MainActor
    func testSaveFailureKeepsDraftAndUnselectedRating() throws {
        let app = launchPreview()
        element("저장 실패", in: app).tap()
        let compose = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 쓰기")).firstMatch
        reveal(compose, in: app)
        compose.tap()
        let content = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 (필수)")).firstMatch
        XCTAssertTrue(content.waitForExistence(timeout: 5))
        content.tap()
        content.typeText("Question A failed draft")
        let quote = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "인상 깊은 문구 (선택)")).firstMatch
        reveal(quote, in: app)
        quote.tap()
        quote.typeText("Question A retained quote")
        let submit = app.buttons.matching(NSPredicate(format: "label == %@", "감상 남기기")).firstMatch
        reveal(submit, in: app)
        submit.tap()
        print("QuestionA helper revision: retry-dialog-close-v3")
        let error = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "개발 프리뷰의 저장 실패 예시입니다.")).firstMatch
        XCTAssertTrue(error.waitForExistence(timeout: 5), app.debugDescription)
        app.buttons.matching(NSPredicate(format: "label == %@", "닫기")).firstMatch.tap()
        reveal(content, in: app, scrollingUp: false)
        XCTAssertEqual(content.value as? String, "Question A failed draft")
        reveal(quote, in: app)
        XCTAssertEqual(quote.value as? String, "Question A retained quote")
        let rating = app.otherElements.matching(NSPredicate(format: "label == %@ AND value != nil", "별점 (선택)")).firstMatch
        reveal(rating, in: app)
        XCTAssertTrue((rating.value as? String)?.contains("미선택") == true, rating.debugDescription)
        attachScreenshot(named: "저장 실패 뒤 미선택과 본문 문구 보존")
    }

    @MainActor
    func testGuestRatingLoginPreservesDraft() throws {
        let app = launchPreview()
        element("게스트", in: app).tap()
        let compose = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 쓰기")).firstMatch
        reveal(compose, in: app)
        compose.tap()
        let content = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 (필수)")).firstMatch
        XCTAssertTrue(content.waitForExistence(timeout: 5))
        content.tap()
        content.typeText("Question A guest draft")
        let rating = app.otherElements.matching(NSPredicate(format: "label == %@ AND value != nil", "별점 (선택)")).firstMatch
        reveal(rating, in: app)
        rating.coordinate(withNormalizedOffset: CGVector(dx: 0.6, dy: 0.5)).tap()
        XCTAssertTrue(element("프리뷰 로그인", in: app).waitForExistence(timeout: 5), app.debugDescription)
        element("회원으로 계속", in: app).tap()
        reveal(content, in: app, scrollingUp: false)
        XCTAssertEqual(content.value as? String, "Question A guest draft")
        reveal(rating, in: app)
        XCTAssertTrue((rating.value as? String)?.contains("미선택") == true, rating.debugDescription)
        rating.coordinate(withNormalizedOffset: CGVector(dx: 0.02, dy: 0.5)).tap()
        XCTAssertTrue((rating.value as? String)?.contains("0.0점") == true, rating.debugDescription)
        attachScreenshot(named: "게스트 로그인 뒤 초안과 별점 선택")
    }

    @MainActor
    func testReferenceBodyBackgroundScrollAndReaction() throws {
        let app = launchPreview()
        let reference = app.buttons["questionReferenceOpen"]
        reveal(reference, in: app)
        let heading = element("여운 한 줄", in: app)
        let backgroundY = heading.frame.minY
        reference.tap()
        let panel = app.otherElements["questionReferencePanel"]
        let body = app.otherElements["book-question-reference-body"]
        XCTAssertTrue(body.waitForExistence(timeout: 5), app.debugDescription)
        let firstParagraph = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@ AND label ENDSWITH %@", "어린 왕자는 작은 별", "(1)")).firstMatch
        XCTAssertTrue(firstParagraph.exists)
        waitForStableFrame(firstParagraph)
        let initialParagraphY = firstParagraph.frame.minY
        body.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.85)).press(forDuration: 0.05, thenDragTo: body.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2)))
        waitForStableFrame(firstParagraph)
        let scrolledY = firstParagraph.frame.minY
        print("Question A body positions: initial=\(initialParagraphY), scrolled=\(scrolledY)")
        XCTAssertLessThan(scrolledY, initialParagraphY - min(20, max(5, body.frame.height * 0.2)))
        XCTAssertEqual(heading.frame.minY, backgroundY, accuracy: 2)
        app.buttons["questionReferenceTabExcerpt"].tap()
        app.buttons["questionReferenceTabSynopsis"].tap()
        let restoreDeadline = Date().addingTimeInterval(5)
        var restoredY = firstParagraph.frame.minY
        while abs(restoredY - scrolledY) >= 3 && Date() < restoreDeadline {
            Thread.sleep(forTimeInterval: 0.2)
            restoredY = firstParagraph.frame.minY
        }
        print("Question A body positions: restored=\(restoredY), expected=\(scrolledY)")
        XCTAssertEqual(restoredY, scrolledY, accuracy: 3)
        attachScreenshot(named: "자료 독립 스크롤과 탭 복원")
        let origin = app.coordinate(withNormalizedOffset: .zero)
        let startY = min(app.frame.maxY - 110, panel.frame.maxY + 250)
        let endY = panel.frame.maxY + 20
        XCTAssertGreaterThan(startY - endY, 30)
        origin.withOffset(CGVector(dx: app.frame.width * 0.9, dy: startY)).press(forDuration: 0.05, thenDragTo: origin.withOffset(CGVector(dx: app.frame.width * 0.9, dy: endY)))
        XCTAssertLessThan(heading.frame.minY, backgroundY - 20)
        XCTAssertTrue(panel.exists)
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "질문과 장면 닫기")).firstMatch.tap()
        let reaction = app.buttons["questionReactionOpen"]
        reveal(reaction, in: app, scrollingUp: false)
        waitForStableFrame(reaction)
        reaction.tap()
        let choice = app.buttons["questionReactionChoice🤔"]
        XCTAssertTrue(choice.waitForExistence(timeout: 5), app.debugDescription)
        choice.tap()
        let clear = app.buttons["questionReactionClear"]
        XCTAssertTrue(clear.waitForExistence(timeout: 5), app.debugDescription)
        XCTAssertTrue(clear.label.contains("🤔"))
        attachScreenshot(named: "이모지 반응 선택")
        clear.tap()
        XCTAssertFalse(clear.exists)
        attachScreenshot(named: "이모지 반응 취소")
    }

    @MainActor
    func testRepresentativeAccessibilityAudit() throws {
        let app = launchPreview()
        let compose = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 쓰기")).firstMatch
        reveal(compose, in: app)
        // 감사 대상 아래의 기존 감상 필터도 화면 안에 온전히 놓는다.
        let origin = app.coordinate(withNormalizedOffset: .zero)
        origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: app.frame.maxY - 120))
            .press(forDuration: 0.05, thenDragTo: origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: app.frame.maxY - 260)))
        reveal(compose, in: app)
        attachScreenshot(named: "질문 카드 접근성 감사")
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion]) { issue in
            print("Question A accessibility issue: \(issue.compactDescription) | \(issue.detailedDescription) | \(issue.element?.debugDescription ?? "no element")")
            return false
        }
        compose.tap()
        let rating = app.otherElements.matching(NSPredicate(format: "label == %@ AND value != nil", "별점 (선택)")).firstMatch
        reveal(rating, in: app)
        attachScreenshot(named: "감상 별점 접근성 감사")
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion]) { issue in
            print("Question A accessibility issue: \(issue.compactDescription) | \(issue.detailedDescription) | \(issue.element?.debugDescription ?? "no element")")
            return false
        }
    }

    @MainActor
    private func launchPreview() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestingBookQuestionA", "-uiTestingExistingInstallation"]
        addUIInterruptionMonitor(withDescription: "시뮬레이터 초기 시스템 안내") { alert in
            if alert.staticTexts["Apple 계정에 로그인"].exists && alert.buttons["취소"].exists {
                alert.buttons["취소"].tap()
                return true
            }
            let keyboardGuide = alert.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "슬라이딩하면서")).firstMatch
            if keyboardGuide.exists && alert.buttons["계속"].exists {
                alert.buttons["계속"].tap()
                return true
            }
            return false
        }
        app.launch()
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let accountPrompt = springboard.alerts.containing(.staticText, identifier: "Apple 계정에 로그인").firstMatch
        if accountPrompt.waitForExistence(timeout: 2) {
            accountPrompt.buttons["취소"].tap()
            app.activate()
        }
        XCTAssertEqual(app.state, .runningForeground)
        XCTAssertTrue(element("질문 A 개발 프리뷰", in: app).waitForExistence(timeout: 15), app.debugDescription)
        return app
    }

    @MainActor
    private func element(_ label: String, in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    @MainActor
    private func reveal(_ element: XCUIElement, in app: XCUIApplication, scrollingUp: Bool = true) {
        var moveUp = scrollingUp
        for attempt in 0..<12 {
            let keyboard = app.keyboards.firstMatch
            let bottom = keyboard.exists ? keyboard.frame.minY - 60 : app.frame.maxY - 34
            let viewportTop = app.frame.minY + 68
            if element.exists {
                let frame = element.frame
                if attempt == 0 { print("질문 A reveal 최초 프레임: \(element.debugDescription)") }
                if element.isHittable && frame.minY >= viewportTop && frame.maxY <= bottom {
                    waitForStableFrame(element)
                    return
                }
                if frame.minY < viewportTop { moveUp = false }
                else if frame.maxY > bottom { moveUp = true }
                else { moveUp = frame.midY >= (viewportTop + bottom) / 2 }
            }
            // 키보드와 여러 줄 입력란이 세로 제스처를 소비하지 않는 시트 오른쪽 여백을 사용한다.
            let top = max(120, bottom - 200)
            let origin = app.coordinate(withNormalizedOffset: .zero)
            let start = origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: moveUp ? bottom - 24 : top))
            let end = origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: moveUp ? top : bottom - 24))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
        XCTAssertTrue(element.exists && element.isHittable, app.debugDescription)
    }

    @MainActor
    private func waitForStableFrame(_ element: XCUIElement) {
        let deadline = Date().addingTimeInterval(10)
        var previousY: CGFloat?
        repeat {
            let y = element.frame.minY
            if let previousY, abs(previousY - y) < 0.5 { return }
            previousY = y
            Thread.sleep(forTimeInterval: 0.2)
        } while Date() < deadline
        XCTFail("요소 스크롤이 정착하지 않았습니다: \(element.debugDescription)")
    }

    @MainActor
    private func attachScreenshot(named name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
