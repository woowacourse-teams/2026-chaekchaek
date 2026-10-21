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
        let compose = app.buttons["questionReviewOpen"]
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
        XCTAssertGreaterThanOrEqual(close.frame.height, 44)
        XCTAssertGreaterThanOrEqual(close.frame.minY, 20)
        tapAfterScroll(close)
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
        let compose = app.buttons["questionReviewOpen"]
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
        let compose = app.buttons["questionReviewOpen"]
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
        let compose = app.buttons["questionReviewOpen"]
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
        let compose = app.buttons["questionReviewOpen"]
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
    func testLibraryButtonReturnsToAccessibilityAndSavesAfterReferenceCloses() throws {
        let app = launchPreview(contentOnly: true)
        let library = element("book_detail_library_save", in: app)
        revealInBook(library, app: app)
        XCTAssertGreaterThanOrEqual(library.frame.height, 44)
        let open = element("questionReferenceOpen", in: app)
        revealInBook(open, app: app)
        tapAfterScroll(open)
        let overlay = element("questionReferencePanel", in: app)
        XCTAssertTrue(overlay.waitForExistence(timeout: 5))
        // 레이아웃 노드는 남아도 가려진 버튼의 역할과 동작은 노출하지 않는다.
        let exposedLibraryButton = app.buttons.matching(identifier: "book_detail_library_save").firstMatch
        XCTAssertTrue(exposedLibraryButton.waitForNonExistence(timeout: 5), "패널이 가린 버튼을 접근성 컨트롤로 노출하면 안 된다")
        try audit(app, name: "자료 패널이 가린 서재 버튼")
        element("questionReferenceClose", in: app).tap()
        XCTAssertTrue(overlay.waitForNonExistence(timeout: 5))
        revealInBook(library, app: app)
        XCTAssertGreaterThanOrEqual(library.frame.width, 44)
        XCTAssertGreaterThanOrEqual(library.frame.height, 44)
        tapAfterScroll(library)
        XCTAssertEqual(library.value as? String, "서재에 저장됨", "다시 노출된 버튼의 실제 터치가 처리되어야 한다")
        try audit(app, name: "자료 닫기 뒤 서재 담기")
        print("QUESTION_A_FLOW_PASS testLibraryButtonReturnsToAccessibilityAndSavesAfterReferenceCloses")
    }

    @MainActor
    func testReviewFiltersKeepFullTouchAreaAboveComposerAndSwitchScope() throws {
        let app = launchPreview(contentOnly: true)
        let all = element("review_scope_all", in: app)
        let mine = element("review_scope_mine", in: app)
        revealInBook(all, app: app)
        let list = element("book_detail_list", in: app)
        XCTAssertLessThanOrEqual(list.frame.maxY, bookFooter(in: app).frame.minY)
        for filter in [all, mine] {
            XCTAssertTrue(filter.isHittable)
            XCTAssertGreaterThanOrEqual(filter.frame.width, 44)
            XCTAssertGreaterThanOrEqual(filter.frame.height, 44)
            XCTAssertLessThanOrEqual(filter.frame.maxY, list.frame.maxY)
        }
        tapAfterScroll(mine)
        XCTAssertTrue(mine.isSelected)
        tapAfterScroll(all)
        XCTAssertTrue(all.isSelected)
        try audit(app, name: "감상 필터 전체 터치 영역과 선택 왕복")
        attachScreenshot(named: "감상 필터와 하단 작성 버튼의 분리된 영역")
        print("QUESTION_A_FLOW_PASS testReviewFiltersKeepFullTouchAreaAboveComposerAndSwitchScope")
    }

    @MainActor
    func testPartiallyCoveredFiltersAndBackReturnAfterReferenceCloses() throws {
        let app = launchPreview(contentOnly: true)
        let open = element("questionReferenceOpen", in: app)
        revealInBook(open, app: app)
        tapAfterScroll(open)
        let overlay = element("questionReferencePanel", in: app)
        XCTAssertTrue(overlay.waitForExistence(timeout: 5))
        let list = element("book_detail_list", in: app)
        let all = element("review_scope_all", in: app)
        // 패널 바깥 왼쪽 여백에서 스크롤해 필터의 위쪽 절반만 가린다.
        for _ in 0..<12 {
            let target = overlay.frame.maxY - 22
            if all.exists && !all.frame.isEmpty && all.frame.minY < overlay.frame.maxY && all.frame.maxY > overlay.frame.maxY { break }
            let delta = all.exists && !all.frame.isEmpty ? all.frame.minY - target : 120
            dragListEdge(list, app: app, delta: min(160, max(-160, delta)))
        }
        XCTAssertTrue(all.exists && !all.frame.isEmpty && all.frame.minY < overlay.frame.maxY && all.frame.maxY > overlay.frame.maxY,
                      "필터가 실제 패널 경계에 걸린 상태에서 접근성을 확인해야 한다")
        attachScreenshot(named: "자료 패널 경계에 걸친 감상 필터")
        let partialTree = XCTAttachment(string: app.debugDescription)
        partialTree.name = "부분 가림 접근성 트리"
        partialTree.lifetime = .keepAlways
        add(partialTree)
        XCTAssertFalse(app.buttons.matching(identifier: "review_scope_all").firstMatch.exists,
                       "일부 가려진 필터를 작은 접근성 버튼으로 노출하면 안 된다")
        try audit(app, name: "부분 가림 필터")
        element("questionReferenceClose", in: app).tap()
        revealInBook(all, app: app)
        let mine = element("review_scope_mine", in: app)
        tapAfterScroll(mine)
        XCTAssertTrue(mine.isSelected)
        tapAfterScroll(all)
        XCTAssertTrue(all.isSelected)

        revealInBook(open, app: app)
        tapAfterScroll(open)
        for _ in 0..<8 { dragListEdge(list, app: app, delta: -180) }
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "뒤로 가기")).firstMatch.exists,
                       "자료 패널에 가려진 뒤로가기를 접근성 버튼으로 노출하면 안 된다")
        try audit(app, name: "패널 아래 책 상단")
        element("questionReferenceClose", in: app).tap()
        let back = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "뒤로 가기")).firstMatch
        XCTAssertTrue(back.waitForExistence(timeout: 5))
        XCTAssertGreaterThanOrEqual(back.frame.height, 44)
        tapAfterScroll(back)
        try audit(app, name: "자료 닫기 뒤 뒤로가기 복원")
        print("QUESTION_A_FLOW_PASS testPartiallyCoveredFiltersAndBackReturnAfterReferenceCloses")
    }

    @MainActor
    func testLandscapeReferenceKeepsReadableBodyAndBackgroundScroll() throws {
        let app = launchPreview(contentOnly: true)
        let open = element("questionReferenceOpen", in: app)
        revealInBook(open, app: app)
        tapAfterScroll(open)
        XCUIDevice.shared.orientation = .landscapeLeft
        defer { XCUIDevice.shared.orientation = .portrait }
        Thread.sleep(forTimeInterval: 1)
        XCTAssertGreaterThan(app.frame.width, app.frame.height, "실제 가로 회전이 적용되어야 한다")
        let overlay = element("questionReferencePanel", in: app)
        XCTAssertTrue(overlay.waitForExistence(timeout: 5))
        let list = element("book_detail_list", in: app)
        let body = element("book-question-reference-body", in: app)
        XCTAssertGreaterThanOrEqual(body.frame.height, 44, "본문 스크롤 영역을 확보해야 한다")
        XCTAssertLessThanOrEqual(overlay.frame.height, 341)
        let bottom = list.frame.maxY - 8
        let top = overlay.frame.maxY + 8
        XCTAssertGreaterThanOrEqual(bottom - top, 44, "패널 아래 목록을 직접 스크롤할 영역을 확보해야 한다")
        // 가려진 컨트롤은 네이티브 접근성 트리에서 없어지므로, 닫힌 상태의 실제 위치로 비교한다.
        element("questionReferenceClose", in: app).tap()
        revealInBook(open, app: app)
        let originalY = open.frame.minY
        tapAfterScroll(open)
        let excerpt = element("questionReferenceTabExcerpt", in: app)
        tapAfterScroll(excerpt)
        let excerptText = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "네 장미꽃을 그토록 소중하게 만든")).firstMatch
        XCTAssertTrue(excerptText.waitForExistence(timeout: 5), "탭 터치가 실제 발췌 본문을 표시해야 한다")
        let excerptY = excerptText.frame.minY
        let bodyBounds = body.frame.intersection(overlay.frame)
        XCTAssertGreaterThanOrEqual(bodyBounds.height, 44)
        attachScreenshot(named: "가로 화면 발췌 본문 스크롤 전")
        print("QUESTION_A_BODY_BEFORE viewport=\(bodyBounds) text=\(excerptText.frame)")
        let screenOrigin = app.coordinate(withNormalizedOffset: .zero)
        for _ in 0..<3 {
            screenOrigin.withOffset(CGVector(dx: bodyBounds.midX, dy: bodyBounds.maxY - 4)).press(forDuration: 0.05,
                thenDragTo: screenOrigin.withOffset(CGVector(dx: bodyBounds.midX, dy: bodyBounds.minY + 4)))
        }
        attachScreenshot(named: "가로 화면 발췌 본문 스크롤 후")
        print("QUESTION_A_BODY_AFTER exists=\(excerptText.exists) text=\(excerptText.frame)")
        XCTAssertTrue(!excerptText.exists || abs(excerptText.frame.minY - excerptY) > 5, "발췌 본문이 독립 스크롤되어야 한다")
        element("questionReferenceClose", in: app).tap()
        XCTAssertEqual(open.frame.minY, originalY, accuracy: 3, "본문 스크롤이 배경으로 전달되면 안 된다")
        tapAfterScroll(open)
        let origin = app.coordinate(withNormalizedOffset: .zero)
        origin.withOffset(CGVector(dx: list.frame.midX, dy: bottom)).press(forDuration: 0.05,
            thenDragTo: origin.withOffset(CGVector(dx: list.frame.midX, dy: top)))
        attachScreenshot(named: "작은 가로 화면의 독립 본문과 배경 목록")
        try audit(app, name: "작은 가로 화면 자료 패널")
        element("questionReferenceClose", in: app).tap()
        XCTAssertTrue(overlay.waitForNonExistence(timeout: 5))
        XCTAssertTrue(!open.exists || abs(open.frame.minY - originalY) > 5, "패널 아래 배경 목록이 실제로 스크롤되어야 한다")
        print("QUESTION_A_FLOW_PASS testLandscapeReferenceKeepsReadableBodyAndBackgroundScroll")
    }

    @MainActor
    private func dragListEdge(_ list: XCUIElement, app: XCUIApplication, delta: CGFloat) {
        let origin = app.coordinate(withNormalizedOffset: .zero)
        let y = list.frame.midY
        origin.withOffset(CGVector(dx: list.frame.minX + 4, dy: y)).press(forDuration: 0.05,
            thenDragTo: origin.withOffset(CGVector(dx: list.frame.minX + 4, dy: y - delta)))
        Thread.sleep(forTimeInterval: 0.5)
    }

    @MainActor
    private func revealInBook(_ control: XCUIElement, app: XCUIApplication) {
        let list = element("book_detail_list", in: app)
        XCTAssertTrue(list.waitForExistence(timeout: 10))
        let footer = bookFooter(in: app)
        XCTAssertTrue(footer.waitForExistence(timeout: 5))
        let top = max(80, list.frame.minY + 40)
        let bottom = footer.frame.minY - 16
        let middle = (top + bottom) / 2
        for _ in 0..<12 {
            if control.exists && control.isHittable && control.frame.minY >= top && control.frame.maxY <= bottom {
                return
            }
            // AX5에서 전체 화면 스와이프가 대상을 상단 밖으로 지나치지 않게 이동량을 제한한다.
            let distance = control.exists && !control.frame.isEmpty ? control.frame.midY - middle : 160
            let delta = min(160, max(-160, distance))
            let origin = app.coordinate(withNormalizedOffset: .zero)
            let start = origin.withOffset(CGVector(dx: list.frame.minX + 8, dy: middle))
            let end = origin.withOffset(CGVector(dx: list.frame.minX + 8, dy: middle - delta))
            start.press(forDuration: 0.05, thenDragTo: end)
            Thread.sleep(forTimeInterval: 0.5)
        }
        XCTAssertTrue(control.isHittable && control.frame.minY >= top && control.frame.maxY <= bottom, app.debugDescription)
    }

    @MainActor
    private func tapAfterScroll(_ control: XCUIElement) {
        // 스크롤을 멈추는 첫 터치와 컨트롤 입력을 혼동하지 않도록 안정화 뒤 한 번 누른다.
        Thread.sleep(forTimeInterval: 0.5)
        control.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).press(forDuration: 0.15)
    }

    @MainActor
    private func bookFooter(in app: XCUIApplication) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "이 순간의 감상 남기기")).firstMatch
    }

    @MainActor
    private func audit(_ app: XCUIApplication, name: String) throws {
        attachScreenshot(named: name)
        try app.performAccessibilityAudit(for: [.dynamicType, .textClipped, .hitRegion]) { issue in
            print("QUESTION_A_AUDIT \(name): \(issue.compactDescription) | \(issue.detailedDescription) | \(issue.element?.debugDescription ?? "nil")")
            return false
        }
    }

    @MainActor
    private func launchPreview(contentOnly: Bool = false) -> XCUIApplication {
        let app = XCUIApplication()
        app.terminate()
        XCUIDevice.shared.orientation = .portrait
        app.launchArguments = ["-uiTestingBookQuestionA", "-uiTestingExistingInstallation"]
        if contentOnly { app.launchArguments.append("-uiTestingBookQuestionAContentOnly") }
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
        XCTAssertTrue(element(contentOnly ? "book_detail_list" : "질문 A 개발 프리뷰", in: app).waitForExistence(timeout: 15), app.debugDescription)
        return app
    }

    @MainActor
    private func element(_ label: String, in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "identifier == %@ OR label == %@", label, label)).firstMatch
    }

    @MainActor
    private func reveal(_ element: XCUIElement, in app: XCUIApplication, scrollingUp: Bool = true) {
        var moveUp = scrollingUp
        for attempt in 0..<12 {
            let keyboard = app.keyboards.firstMatch
            let footer = bookFooter(in: app)
            let list = self.element("book_detail_list", in: app)
            let bookBottom = footer.exists && footer.isHittable && list.exists ? list.frame.maxY - 16 : app.frame.maxY - 34
            let bottom = keyboard.exists ? keyboard.frame.minY - 60 : bookBottom
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
            Thread.sleep(forTimeInterval: 0.5)
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
