import XCTest

/// Debug 전용 시안이다. 서버 저장을 검증하지 않는다.
/// 같은 suite를 기본 Dynamic Type와 AX5에서 각각 실행한다.
final class BookQuestionUITests: XCTestCase {
    private var accessibilityFailures: [String] = []

    override func setUpWithError() throws {
        continueAfterFailure = false
        accessibilityFailures = []
    }

    override func tearDownWithError() throws {
        if !accessibilityFailures.isEmpty {
            XCTFail("기능 흐름과 별도로 수집한 접근성 감사 실패:\n" + accessibilityFailures.joined(separator: "\n"))
        }
    }

    @MainActor
    func testBookWithoutQuestionKeepsReviewEntry() throws {
        let app = launchPreview(questionVisible: false)
        XCTAssertTrue(element("book_detail_list", in: app).waitForExistence(timeout: 10))
        XCTAssertFalse(element("book_question", in: app).exists)
        XCTAssertFalse(element("question_reference_open", in: app).exists)
        XCTAssertFalse(element("question_reaction", in: app).exists)
        let compose = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "이 순간의 감상 남기기")).firstMatch
        XCTAssertTrue(compose.waitForExistence(timeout: 5))
        XCTAssertTrue(compose.isHittable)
        try audit(app, name: "질문 없는 책 상세")
        print("QUESTION_A_FLOW_PASS testBookWithoutQuestionKeepsReviewEntry")
    }

    @MainActor
    func testReferenceTabsAndBothScrollAreasKeepTheirOwnPosition() throws {
        let app = launchPreview()
        let open = element("question_reference_open", in: app)
        revealInBook(open, app: app)
        let originalY = open.frame.minY
        open.tap()
        let overlay = element("question_reference_overlay", in: app)
        XCTAssertTrue(overlay.waitForExistence(timeout: 5))
        XCTAssertEqual(open.frame.minY, originalY, accuracy: 3, "자료 열기가 뒤 목록을 이동시키면 안 된다")
        let overlayFrame = overlay.frame
        XCTAssertLessThanOrEqual(overlayFrame.height, 342)
        XCTAssertGreaterThanOrEqual(overlayFrame.height, 278)

        let excerpt = element("question_tab_Excerpt", in: app)
        XCTAssertTrue(excerpt.isHittable)
        excerpt.tap()
        let body = element("question_reference_body", in: app)
        XCTAssertTrue(body.waitForExistence(timeout: 5))
        body.swipeUp()
        XCTAssertEqual(open.frame.minY, originalY, accuracy: 3, "본문 스크롤이 뒤 목록에 전달되면 안 된다")
        XCTAssertEqual(overlay.frame.minY, overlayFrame.minY, accuracy: 1)
        element("question_tab_Plot", in: app).tap()
        XCTAssertTrue(body.waitForExistence(timeout: 5))
        let plotHeading = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "어린 왕자와 여우")).firstMatch
        XCTAssertTrue(plotHeading.waitForExistence(timeout: 5), app.debugDescription)
        let headingY = plotHeading.frame.minY
        body.swipeUp()
        XCTAssertTrue(!plotHeading.exists || abs(plotHeading.frame.minY - headingY) > 10,
                      "참고 본문 자체가 스크롤되어야 한다")
        XCTAssertEqual(open.frame.minY, originalY, accuracy: 3)
        try audit(app, name: "상단 자료 패널과 탭")

        // 패널 밖의 실제 목록 영역에서만 드래그해 비차단 동작을 확인한다.
        let list = element("book_detail_list", in: app)
        let bottom = min(list.frame.maxY, bookFooter(in: app).frame.minY) - 16
        let top = overlay.frame.maxY + 20
        XCTAssertGreaterThan(bottom - top, 60)
        let origin = app.coordinate(withNormalizedOffset: .zero)
        let start = origin.withOffset(CGVector(dx: list.frame.midX, dy: bottom))
        let end = origin.withOffset(CGVector(dx: list.frame.midX, dy: top))
        start.press(forDuration: 0.05, thenDragTo: end)
        XCTAssertTrue(!open.exists || abs(open.frame.minY - originalY) > 10, "패널 아래 감상 목록이 계속 스크롤되어야 한다")

        let visibleReview = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "서로에게 특별해지는"))
            .allElementsBoundByIndex.first { $0.frame.minY > overlay.frame.maxY && $0.frame.minY < bottom }
        let anchor = try XCTUnwrap(visibleReview, "패널 아래 감상 위치를 확인할 수 없다")
        let anchorLabel = anchor.label
        let anchorY = anchor.frame.minY
        element("question_reference_close", in: app).tap()
        XCTAssertTrue(overlay.waitForNonExistence(timeout: 5))
        let restoredAnchor = app.staticTexts.matching(NSPredicate(format: "label == %@", anchorLabel)).firstMatch
        XCTAssertTrue(restoredAnchor.exists)
        XCTAssertEqual(restoredAnchor.frame.minY, anchorY, accuracy: 3, "자료 닫기가 뒤 목록을 이동시키면 안 된다")
        attachScreenshot(named: "자료 닫기 뒤 감상 목록 위치")
        print("QUESTION_A_FLOW_PASS testReferenceTabsAndBothScrollAreasKeepTheirOwnPosition")
    }

    @MainActor
    func testEmojiPickerSelectsReplacesAndCancelsReaction() throws {
        let app = launchPreview()
        let reaction = element("question_reaction", in: app)
        revealInBook(reaction, app: app)
        XCTAssertFalse(reaction.label.isEmpty)
        XCTAssertGreaterThanOrEqual(reaction.frame.width, 44)
        XCTAssertGreaterThanOrEqual(reaction.frame.height, 44)
        reaction.tap()
        let picker = element("question_reaction_picker", in: app)
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        for option in ["ThumbsUp", "Heart", "Thinking", "Relaxed", "Moved", "Sparkles"] {
            let choice = element("question_reaction_choice_\(option)", in: app)
            XCTAssertTrue(choice.exists)
            XCTAssertFalse(choice.label.isEmpty)
            XCTAssertGreaterThanOrEqual(choice.frame.width, 44)
            XCTAssertGreaterThanOrEqual(choice.frame.height, 44)
        }
        try audit(app, name: "질문 이모지 선택기 6개 반응")
        element("question_reaction_choice_ThumbsUp", in: app).tap()
        let selected = element("question_reaction_selected", in: app)
        XCTAssertTrue(selected.waitForExistence(timeout: 5))
        XCTAssertFalse(selected.label.isEmpty)
        XCTAssertFalse(picker.exists)
        XCTAssertTrue(reaction.exists)
        XCTAssertGreaterThanOrEqual(selected.frame.width, 44)
        XCTAssertGreaterThanOrEqual(selected.frame.height, 44)
        let firstSelectionLabel = selected.label
        reaction.tap()
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        element("question_reaction_choice_Heart", in: app).tap()
        XCTAssertTrue(selected.waitForExistence(timeout: 5))
        XCTAssertNotEqual(selected.label, firstSelectionLabel, "바꾼 반응을 접근성 이름으로 구별할 수 있어야 한다")
        try audit(app, name: "선택된 이모지와 취소 pill")
        selected.tap()
        XCTAssertTrue(selected.waitForNonExistence(timeout: 5))
        XCTAssertTrue(reaction.exists)
        XCTAssertFalse(picker.exists)
        print("QUESTION_A_FLOW_PASS testEmojiPickerSelectsReplacesAndCancelsReaction")
    }

    @MainActor
    func testReviewDraftSurvivesContinueWritingAndSubmitsActualZero() throws {
        let app = launchPreview()
        let open = element("question_review_open", in: app)
        revealInBook(open, app: app)
        open.tap()
        let content = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "감상 (필수)")).firstMatch
        XCTAssertTrue(content.waitForExistence(timeout: 5))
        XCTAssertFalse(app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "쪽수")).firstMatch.exists)
        XCTAssertFalse(app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "목차 또는 챕터")).firstMatch.exists)
        let quote = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "인상 깊은 문구")).firstMatch
        revealInReview(quote, app: app, upward: true)
        quote.tap()
        quote.typeText("A remembered line")
        revealInReview(content, app: app, upward: false)
        content.tap()
        content.typeText("Time made this special")
        let close = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "닫기")).firstMatch
        revealInReview(close, app: app, upward: false)
        close.tap()
        let keepWriting = app.buttons["계속 작성"]
        XCTAssertTrue(keepWriting.waitForExistence(timeout: 5))
        keepWriting.tap()
        XCTAssertTrue(content.waitForExistence(timeout: 5))
        XCTAssertEqual(content.value as? String, "Time made this special")
        XCTAssertEqual(quote.value as? String, "A remembered line")

        let rating = app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", "별점 선택")).firstMatch
        revealInReview(rating, app: app, upward: true)
        XCTAssertTrue(rating.exists, app.debugDescription)
        XCTAssertTrue((rating.value as? String ?? "").hasPrefix("0"), "초기 별점은 실제 0점이어야 한다")
        // iOS에서는 별점이 Other로 노출되므로 실제 탭과 드래그 입력으로 검사한다.
        rating.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5)).tap()
        XCTAssertFalse((rating.value as? String ?? "").hasPrefix("0"), "오른쪽 탭으로 실제 점수가 바뀌어야 한다")
        let ratingEnd = rating.coordinate(withNormalizedOffset: CGVector(dx: 1, dy: 0.5))
        let ratingZero = rating.coordinate(withNormalizedOffset: CGVector(dx: -0.1, dy: 0.5))
        ratingEnd.press(forDuration: 0.05, thenDragTo: ratingZero)
        XCTAssertTrue((rating.value as? String ?? "").hasPrefix("0"), "0점은 실제 조절 가능한 최솟값이어야 한다")
        let save = app.buttons["감상 남기기"]
        revealInReview(save, app: app, upward: true)
        XCTAssertTrue(save.isEnabled)
        attachScreenshot(named: "감상 입력 보존과 실제 0점")
        // 기존 저장 실패 검사와 같은 단일 터치 조건으로 Compose 입력을 전달한다.
        Thread.sleep(forTimeInterval: 0.5)
        print("QUESTION_A_SAVE_BEFORE frame=\(save.frame) enabled=\(save.isEnabled) value=\(rating.value ?? "nil")")
        save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).press(forDuration: 0.15)
        XCTAssertTrue(app.staticTexts["시안에서 입력을 확인했어요."].waitForExistence(timeout: 5), app.debugDescription)
        XCTAssertFalse(content.exists)
        try audit(app, name: "시안 감상 제출 뒤 상세 복귀")
        print("QUESTION_A_FLOW_PASS testReviewDraftSurvivesContinueWritingAndSubmitsActualZero")
    }

    @MainActor
    private func launchPreview(questionVisible: Bool = true) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestingBookQuestion", "-uiTestingExistingInstallation"]
        if !questionVisible { app.launchArguments.append("-uiTestingBookQuestionAbsent") }
        app.launch()
        return app
    }

    @MainActor
    private func element(_ identifier: String, in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }

    @MainActor
    private func revealInBook(_ control: XCUIElement, app: XCUIApplication) {
        let list = element("book_detail_list", in: app)
        XCTAssertTrue(list.waitForExistence(timeout: 10))
        let footer = bookFooter(in: app)
        XCTAssertTrue(footer.waitForExistence(timeout: 5))
        for _ in 0..<6 {
            if control.exists && control.isHittable && control.frame.maxY < footer.frame.minY - 8 { return }
            list.swipeUp()
        }
        XCTAssertTrue(control.isHittable, app.debugDescription)
    }

    @MainActor
    private func bookFooter(in app: XCUIApplication) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "이 순간의 감상 남기기")).firstMatch
    }

    @MainActor
    private func revealInReview(_ control: XCUIElement, app: XCUIApplication, upward: Bool) {
        for _ in 0..<7 {
            let keyboard = app.keyboards.firstMatch
            let bottom = keyboard.exists ? keyboard.frame.minY - 60 : app.frame.maxY - 45
            if control.exists && control.isHittable && control.frame.height >= 40 && control.frame.maxY <= bottom { return }
            // 입력란과 키보드가 제스처를 소비하므로 시트 오른쪽 여백을 사용한다.
            let top = max(170, bottom - 280)
            let origin = app.coordinate(withNormalizedOffset: .zero)
            let start = origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: upward ? bottom - 20 : top))
            let end = origin.withOffset(CGVector(dx: app.frame.width * 0.97, dy: upward ? top : bottom - 20))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
        XCTAssertTrue(control.isHittable, app.debugDescription)
    }

    @MainActor
    private func audit(_ app: XCUIApplication, name: String) throws {
        attachScreenshot(named: name)
        do {
            try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion]) { issue in
                let element = issue.element
                let details = "\(name): \(issue.compactDescription) | \(issue.detailedDescription) | element=\(element?.debugDescription ?? "nil") | frame=\(String(describing: element?.frame))"
                self.accessibilityFailures.append(details)
                print("QUESTION_A_AUDIT \(details)")
                let attachment = XCTAttachment(string: details)
                attachment.name = "접근성 감사: \(name)"
                attachment.lifetime = .keepAlways
                self.add(attachment)
                // 기능 왕복을 계속 검사하되 tearDown에서 감사를 실패로 보고한다.
                return true
            }
        } catch {
            let details = "\(name): 감사 실행 오류 \(error)"
            accessibilityFailures.append(details)
            print("QUESTION_A_AUDIT \(details)")
        }
    }

    @MainActor
    private func attachScreenshot(named name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
