import XCTest
import Network
import UIKit

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
        XCTAssertFalse(app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "목차 또는 챕터")).firstMatch.exists)
        let quote = app.textViews.matching(NSPredicate(format: "label BEGINSWITH %@", "인상 깊은 문구")).firstMatch
        revealReviewControl(quote, in: app, scrollingUp: false)
        quote.tap()
        quote.typeText("saved quote")
        revealReviewControl(content, in: app, scrollingUp: false)
        content.tap()
        content.typeText("draft must survive")
        let spoiler = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "스포일러 (선택)")).firstMatch
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
        attachScreenshot(named: "감상 저장 실패 후 입력 보존")
        revealReviewControl(save, in: app, scrollingUp: true)
        try await Task.sleep(for: .milliseconds(500))
        save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).press(forDuration: 0.15)
        XCTAssertTrue(retry.waitForExistence(timeout: 5))
        retry.tap()
        XCTAssertFalse(save.isEnabled)
        let closed = NSPredicate(format: "exists == false")
        let closedExpectation = expectation(for: closed, evaluatedWith: content)
        await fulfillment(of: [closedExpectation], timeout: 10)
        let (savedData, _) = try await URLSession.shared.data(from: base.appendingPathComponent("__review-draft-qa/state"))
        let saved = try XCTUnwrap(JSONSerialization.jsonObject(with: savedData) as? [String: [[String: Any]]])
        XCTAssertEqual(saved["requests"]?.count, 3)
        let submissions = try XCTUnwrap(saved["requests"])
        XCTAssertEqual(submissions.first?["content"] as? String, "draft must survive")
        XCTAssertEqual(submissions.last?["quote"] as? String, "saved quote")
        XCTAssertNil(submissions.last?["chapter"] as? String)
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
            let edit = app.buttons["로그인"]
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

    @MainActor
    func testMissingNicknameCannotDismissAndCompletesLoginAfterSave() throws {
        let fixture = try NicknameFixtureServer()
        defer { fixture.stop() }
        for restored in [false, true] {
            fixture.reset()
            let app = XCUIApplication()
            app.launchArguments = ["-uiTestingGuest", "-uiTestingRefresh", "-uiTestingNickname"]
            if restored { app.launchArguments.append("-uiTestingNicknameRestore") }
            app.launchEnvironment["CHAEKCHAEK_UI_TEST_API_URL"] = fixture.baseURL
            app.launch()
            if !restored {
                let shelf = app.buttons["내 서재"]
                XCTAssertTrue(shelf.waitForExistence(timeout: 15))
                shelf.tap()
                app.buttons["로그인"].tap()
                let google = app.buttons["GIDSignInButton"]
                XCTAssertTrue(google.waitForExistence(timeout: 5))
                google.tap()
            }
            let title = app.staticTexts["닉네임 설정"]
            XCTAssertTrue(title.waitForExistence(timeout: 10))
            // 필수 창 바깥을 눌러도 유지한다.
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15)).tap()
            XCTAssertTrue(title.exists)
            let field = app.textFields.firstMatch.exists ? app.textFields.firstMatch : app.textViews.firstMatch
            XCTAssertTrue(field.waitForExistence(timeout: 5), app.debugDescription)
            field.tap()
            field.typeText("reader")
            app.buttons["확인"].tap()
            let failure = app.staticTexts["설정을 변경하지 못했어요"]
            XCTAssertTrue(failure.waitForExistence(timeout: 5))
            app.buttons["닫기"].tap()
            XCTAssertTrue(field.waitForExistence(timeout: 5))
            XCTAssertEqual(field.value as? String, "reader")
            attachScreenshot(named: restored ? "기존 회원 닉네임 실패 입력 유지" : "신규 회원 닉네임 실패 입력 유지")
            app.buttons["확인"].tap()
            XCTAssertTrue(title.waitForNonExistence(timeout: 10))
            XCTAssertEqual(fixture.nicknameWrites, 2)
            app.terminate()
        }
    }

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testGoogleButtonCornersKeepLoginSheetBackgroundAfterReentry() throws {
        try verifyGoogleButtonCorners(preservingSession: false)
    }

    @MainActor
    func testGoogleButtonCornersPreservingDeviceSession() throws {
        try verifyGoogleButtonCorners(preservingSession: true)
    }

    @MainActor
    private func verifyGoogleButtonCorners(preservingSession: Bool) throws {
        let app = XCUIApplication()
        if !preservingSession { app.launchArguments.append("-uiTestingGuest") }
        app.launch()
        let profile = app.buttons["내 프로필"]
        XCTAssertTrue(profile.waitForExistence(timeout: 15))

        for attempt in 1...2 {
            profile.tap()
            let google = app.buttons["GIDSignInButton"]
            XCTAssertTrue(google.waitForExistence(timeout: 5))
            XCTAssertTrue(google.isHittable)
            let screenshot = XCUIScreen.main.screenshot()
            let attachment = XCTAttachment(screenshot: screenshot)
            attachment.name = "홈 프로필 Google 버튼 모서리 진입 \(attempt)"
            attachment.lifetime = .keepAlways
            add(attachment)
            let image = try XCTUnwrap(screenshot.image.cgImage)
            let scale = CGFloat(image.width) / app.frame.width
            let frame = google.frame
            // 8pt 둥근 테두리 바깥, 네이티브 뷰 사각형 안의 네 모서리는 시트의 흰색이어야 한다.
            let corners = [
                CGPoint(x: frame.minX + 1, y: frame.minY + 1),
                CGPoint(x: frame.maxX - 1, y: frame.minY + 1),
                CGPoint(x: frame.minX + 1, y: frame.maxY - 1),
                CGPoint(x: frame.maxX - 1, y: frame.maxY - 1),
            ]
            for point in corners {
                let pixel = try XCTUnwrap(image.cropping(to: CGRect(
                    x: floor(point.x * scale), y: floor(point.y * scale), width: 1, height: 1
                )))
                var rgba = [UInt8](repeating: 0, count: 4)
                let context = try XCTUnwrap(CGContext(
                    data: &rgba, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4,
                    space: CGColorSpaceCreateDeviceRGB(),
                    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
                ))
                context.draw(pixel, in: CGRect(x: 0, y: 0, width: 1, height: 1))
                XCTAssertTrue(rgba[0] > 230 && rgba[1] > 230 && rgba[2] > 230,
                              "Google 버튼 모서리가 시트 배경과 다르다: \(point), RGB \(rgba)")
            }
            app.buttons["닫기"].firstMatch.tap()
            XCTAssertTrue(profile.waitForExistence(timeout: 5))
        }
    }

    @MainActor
    func testNativeSignInButtonsAppearInLoginSheet() throws {
        let app = XCUIApplication()
        app.launchArguments.append("-uiTestingGuest")
        app.launch()

        let shelfTab = app.buttons["내 서재"]
        XCTAssertTrue(shelfTab.waitForExistence(timeout: 10))
        shelfTab.tap()

        let editButton = app.buttons["로그인"]
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
    func testHomePullRefreshHidesListEndAndKeepsNavigationVisible() throws {
        let fixture = try RefreshFixtureServer()
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        XCTAssertTrue(app.buttons["홈"].waitForExistence(timeout: 15))
        let end = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "여기까지 모두 봤어요")).firstMatch
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "새로고침 검증 도서")).firstMatch.waitForExistence(timeout: 15))
        for _ in 0..<8 { app.swipeUp() }
        XCTAssertFalse(end.exists)
        attachScreenshot(named: "홈 하단 끝 안내 없음")
        app.buttons["홈"].tap()
        let requestsBeforePull = fixture.completedRequests(for: "/api/v1/home/latest-reviews")
        XCTAssertGreaterThan(requestsBeforePull, 0)
        let shortPullStart = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.45))
        let shortPullEnd = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.50))
        shortPullStart.press(forDuration: 0.1, thenDragTo: shortPullEnd)
        let unexpectedRefresh = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            fixture.completedRequests(for: "/api/v1/home/latest-reviews") > requestsBeforePull
        }, object: nil)
        unexpectedRefresh.isInverted = true
        XCTAssertEqual(XCTWaiter.wait(for: [unexpectedRefresh], timeout: 1), .completed,
                       "짧은 당김은 API를 다시 요청하지 않아야 합니다")
        app.swipeDown()
        XCTAssertTrue(refreshLoadingLabel(in: app).waitForExistence(timeout: 3))
        attachScreenshot(named: "홈 단순 로더와 안내 문구")
        waitForNewResponse(fixture, path: "/api/v1/home/latest-reviews", after: requestsBeforePull)
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertEqual(fixture.completedRequests(for: "/api/v1/home/latest-reviews"), requestsBeforePull + 1)
        XCTAssertTrue(app.buttons["내 프로필"].isHittable)
        XCTAssertTrue(app.buttons["발견"].exists)
        XCTAssertTrue(app.buttons["내 서재"].exists)
        attachScreenshot(named: "홈 당김 후 기존 화면")
        // hitRegion은 시스템 홈 인디케이터에서도 실패해 별도 검증 기록으로 남긴다.
        try app.performAccessibilityAudit(for: [.textClipped])
    }

    @MainActor
    func testSearchOmitsMissingYearsAndExcludesProductsWithoutIsbn() throws {
        let fixture = try RefreshFixtureServer(searchMetadata: true)
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        XCTAssertTrue(app.buttons["발견"].waitForExistence(timeout: 15))
        app.buttons["발견"].tap()
        let field = app.textViews["search-query-field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText("rye\n")
        XCTAssertTrue(app.staticTexts["출판일 없는 호밀 도서"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.staticTexts["출판일 누락 도서"].exists)
        XCTAssertFalse(app.staticTexts["ISBN 없는 세트"].exists)
        XCTAssertFalse(app.staticTexts["ID 없는 도서"].exists)
        XCTAssertFalse(app.staticTexts["ID 누락 도서"].exists)
        XCTAssertFalse(app.staticTexts["출판일 정보 없음"].exists)
        XCTAssertFalse(app.buttons["다시 시도"].exists)
        XCTAssertEqual(fixture.completedRequests(for: "/api/v1/books"), 1)
        attachScreenshot(named: "검색 출판일 생략과 ISBN 없는 상품 제외")
    }

    @MainActor
    func testEmptySearchPullKeepsSubmittedInput() throws {
        let fixture = try RefreshFixtureServer()
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        XCTAssertTrue(app.buttons["발견"].waitForExistence(timeout: 15))
        app.buttons["발견"].tap()
        let field = app.textViews["search-query-field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        let query = "zzxqrefresh529nomatch"
        // 부하가 큰 Simulator의 연속 입력 누락과 새로고침의 입력 보존 실패를 구분한다.
        for character in query { field.typeText(String(character)) }
        guard field.value as? String == query else {
            XCTFail("새로고침 전에 테스트 검색어가 정확히 입력되어야 합니다")
            return
        }
        field.typeText("\n")
        let empty = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "검색 결과가 없어요")).firstMatch
        XCTAssertTrue(empty.waitForExistence(timeout: 15))
        let requestsBeforePull = fixture.completedRequests(for: "/api/v1/books")
        XCTAssertEqual(requestsBeforePull, 1)
        app.swipeDown()
        XCTAssertTrue(refreshLoadingLabel(in: app).waitForExistence(timeout: 3))
        attachScreenshot(named: "빈 검색 단순 로더와 안내 문구")
        waitForNewResponse(fixture, path: "/api/v1/books", after: requestsBeforePull)
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertEqual(fixture.completedRequests(for: "/api/v1/books"), requestsBeforePull + 1)
        XCTAssertTrue(empty.exists)
        XCTAssertEqual(field.value as? String, query)
        attachScreenshot(named: "빈 검색 결과 당김 후 검색어 유지")
        // hitRegion은 시스템 홈 인디케이터에서도 실패해 별도 검증 기록으로 남긴다.
        try app.performAccessibilityAudit(for: [.textClipped])
    }

    @MainActor
    func testEmptyFeedPullRefreshesWithoutListEnd() throws {
        let fixture = try RefreshFixtureServer()
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        XCTAssertTrue(app.buttons["피드"].waitForExistence(timeout: 15))
        app.buttons["피드"].tap()
        let empty = app.staticTexts.matching(
            NSPredicate(format: "label CONTAINS %@", "아직 도착한 감상이 없어요")
        ).firstMatch
        XCTAssertTrue(empty.waitForExistence(timeout: 15))
        let path = "/api/v1/feed/reviews"
        let requestsBeforePull = fixture.completedRequests(for: path)
        XCTAssertGreaterThan(requestsBeforePull, 0)
        app.swipeDown()
        XCTAssertTrue(refreshLoadingLabel(in: app).waitForExistence(timeout: 3))
        attachScreenshot(named: "빈 피드 단순 로더와 안내 문구")
        waitForNewResponse(fixture, path: path, after: requestsBeforePull)
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertEqual(fixture.completedRequests(for: path), requestsBeforePull + 1)
        XCTAssertTrue(empty.exists)
        XCTAssertFalse(app.staticTexts.matching(
            NSPredicate(format: "label CONTAINS %@", "여기까지 모두 봤어요")
        ).firstMatch.exists)
        attachScreenshot(named: "빈 피드 당김 후 기존 화면")
        try app.performAccessibilityAudit(for: [.textClipped])
    }

    @MainActor
    func testBookDetailPullKeepsContentAndRefreshesReviews() throws {
        try verifyBookDetailPull(initialReviews: true)
    }

    @MainActor
    func testEmptyBookDetailReviewsCanBeRefreshed() throws {
        try verifyBookDetailPull(initialReviews: false)
    }

    @MainActor
    private func verifyBookDetailPull(initialReviews: Bool) throws {
        let fixture = try RefreshFixtureServer(initialDetailReviews: initialReviews)
        defer { fixture.stop() }
        let app = refreshApplication(fixture)
        app.launch()
        let title = app.descendants(matching: .any).matching(NSPredicate(
            format: "label CONTAINS %@", "새로고침 검증 도서"
        )).firstMatch
        XCTAssertTrue(title.waitForExistence(timeout: 15))
        title.tap()
        let detailPath = "/api/v1/books/by-isbn/9780000000001"
        let reviewsPath = "/api/v1/books/1/reviews"
        waitForNewResponse(fixture, path: reviewsPath, after: 0)
        XCTAssertTrue(app.buttons["뒤로 가기"].waitForExistence(timeout: 5))
        let initialContent = initialReviews ? "기존 상세 감상" : "아직 등록된 감상이 없어요"
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", initialContent))
            .firstMatch.waitForExistence(timeout: 10))
        fixture.beginDetailRefresh()
        let detailBefore = fixture.completedRequests(for: detailPath)
        let reviewsBefore = fixture.completedRequests(for: reviewsPath)
        app.swipeDown()
        XCTAssertTrue(refreshLoadingLabel(in: app).waitForExistence(timeout: 3))
        XCTAssertTrue(title.exists)
        attachScreenshot(named: "책 상세 C 새로고침 \(initialReviews ? "기존 감상" : "빈 감상")")
        waitForNewResponse(fixture, path: reviewsPath, after: reviewsBefore)
        waitForRefreshIndicatorToClose(in: app)
        XCTAssertEqual(fixture.completedRequests(for: detailPath), detailBefore + 1)
        XCTAssertEqual(fixture.completedRequests(for: reviewsPath), reviewsBefore + 1)
        let updated = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "갱신한 상세 감상")).firstMatch
        for _ in 0..<4 where !updated.isHittable { app.swipeUp() }
        XCTAssertTrue(updated.isHittable)
        attachScreenshot(named: "책 상세 갱신 후 감상")
    }

    @MainActor
    private func refreshApplication(_ fixture: RefreshFixtureServer) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-uiTestingGuest", "-uiTestingRefresh"]
        app.launchEnvironment["CHAEKCHAEK_UI_TEST_API_URL"] = fixture.baseURL
        return app
    }

    @MainActor
    private func waitForNewResponse(_ fixture: RefreshFixtureServer, path: String, after count: Int) {
        // 응답이 한 프레임 안에 끝나도 누적 횟수는 남는다. 콜백이 끊기면 반드시 시간 초과된다.
        let response = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            fixture.completedRequests(for: path) > count
        }, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [response], timeout: 10), .completed,
                       "당김 후 실제 API 응답이 완료되어야 합니다: \(path)")
    }

    @MainActor
    private func refreshIndicator(in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(
            format: "identifier == %@ OR label IN %@", "pull-refresh-indicator",
            ["당겨서 새로고침", "놓으면 새로고침", "갱신 요청 중", "새로고침 중이에요"]
        )).firstMatch
    }

    @MainActor
    private func refreshLoadingLabel(in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(
            format: "label == %@", "새로고침 중이에요"
        )).firstMatch
    }

    @MainActor
    private func waitForRefreshIndicatorToClose(in app: XCUIApplication) {
        expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: refreshIndicator(in: app))
        waitForExpectations(timeout: 10)
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

/// XCUITest 프로세스에서 응답과 횟수를 소유한다. 앱에는 화면용 진단 요소를 추가하지 않는다.
private final class RefreshFixtureServer: @unchecked Sendable {
    private let listener: NWListener
    private let queue = DispatchQueue(label: "chaekchaek.refresh-ui-test-api")
    private let lock = NSLock()
    private var responses: [String: Int] = [:]
    private let searchMetadata: Bool
    private let initialDetailReviews: Bool
    private var detailRefreshArmed = false

    var baseURL: String { "http://127.0.0.1:\(listener.port!.rawValue)" }

    init(initialDetailReviews: Bool = false, searchMetadata: Bool = false) throws {
        self.searchMetadata = searchMetadata
        self.initialDetailReviews = initialDetailReviews
        listener = try NWListener(using: .tcp, on: .any)
        let ready = XCTestExpectation(description: "로컬 API 서버 시작")
        listener.stateUpdateHandler = { state in
            if case .ready = state { ready.fulfill() }
        }
        listener.newConnectionHandler = { [weak self] connection in
            guard let self else { return }
            connection.start(queue: self.queue)
            self.receive(connection)
        }
        listener.start(queue: queue)
        guard XCTWaiter.wait(for: [ready], timeout: 5) == .completed else {
            listener.cancel()
            throw NSError(domain: "RefreshFixtureServer", code: 1)
        }
    }

    func completedRequests(for path: String) -> Int {
        lock.lock()
        defer { lock.unlock() }
        return responses[path, default: 0]
    }

    func beginDetailRefresh() {
        lock.lock()
        detailRefreshArmed = true
        lock.unlock()
    }

    private var isDetailRefresh: Bool {
        lock.lock()
        defer { lock.unlock() }
        return detailRefreshArmed
    }

    func stop() { listener.cancel() }

    private func receive(_ connection: NWConnection, buffered: Data = Data()) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 65_536) { [weak self] data, _, complete, error in
            guard let self else { return }
            let request = buffered + (data ?? Data())
            guard String(data: request, encoding: .utf8)?.contains("\r\n\r\n") == true else {
                if !complete && error == nil { self.receive(connection, buffered: request) }
                else { connection.cancel() }
                return
            }
            let target = String(data: request, encoding: .utf8)?.components(separatedBy: " ").dropFirst().first ?? ""
            let path = URLComponents(string: "http://127.0.0.1\(target)")?.path ?? ""
            let body = self.response(for: path)
            let header = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: \(body.count)\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n"
            // 두 번째 조회부터 지연시켜 실제 표시와 응답 후 접힘을 검증한다.
            let detailRequest = path == "/api/v1/books/1/reviews" || path == "/api/v1/books/by-isbn/9780000000001"
            let delayed = detailRequest ? self.isDetailRefresh : self.completedRequests(for: path) > 0
            let delay = delayed ? 2.0 : 0.0
            self.queue.asyncAfter(deadline: .now() + delay) {
                connection.send(content: Data(header.utf8) + body, completion: .contentProcessed { error in
                    if error == nil {
                        self.lock.lock()
                        self.responses[path, default: 0] += 1
                        self.lock.unlock()
                    }
                    connection.cancel()
                })
            }
        }
    }

    private func response(for path: String) -> Data {
        let body: String
        switch path {
        case "/api/v1/books" where searchMetadata:
            body = """
            {"totalCount":5,"nextPage":null,"items":[
              {"bookId":null,"title":"ID 없는 도서","coverImageUrl":"","authors":[],"translators":[],"publisher":"출판사","category":"소설","isbn13":"9780000000001"},
              {"title":"ID 누락 도서","coverImageUrl":"","authors":[],"translators":[],"publisher":"출판사","category":"소설","isbn13":"9780000000002"},
              {"bookId":41,"title":"출판일 없는 호밀 도서","coverImageUrl":"","authors":["저자"],"translators":[],"publisher":"출판사","category":"소설","publishedDate":null,"isbn13":"9788937460449"},
              {"bookId":42,"title":"출판일 누락 도서","coverImageUrl":"","authors":["저자"],"translators":[],"publisher":"출판사","category":"소설","isbn13":"9788936433598"},
              {"bookId":43,"title":"ISBN 없는 세트","coverImageUrl":"","authors":["저자"],"translators":[],"publisher":"출판사","category":"소설","publishedDate":null,"isbn13":""}
            ]}
            """
        case "/api/v1/books/by-isbn/9780000000001":
            body = """
            {"bookId":1,"isbn13":"9780000000001","title":"새로고침 검증 도서","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","myRecord":null}
            """
        case "/api/v1/books/1/reviews":
            let refreshed = isDetailRefresh
            if !initialDetailReviews && !refreshed {
                body = "{\"totalCount\":0,\"nextPage\":null,\"items\":[]}"
            } else {
                let content = refreshed ? "갱신한 상세 감상" : "기존 상세 감상"
                body = """
                {"totalCount":1,"nextPage":null,"items":[{"reviewId":7,"content":"\(content)","createdAt":"2026-10-21T00:00:00Z","author":{"displayName":"검증 독자","anonymous":false,"mine":false,"actorType":"GUEST"},"replyCount":0,"likeCount":0,"likedByMe":false,"isSpoiler":false,"recentReplies":[],"deleted":false}]}
                """
            }

        case "/api/v1/home/popular-books":
            body = """
            {"books":[{"bookId":1,"isbn13":"9780000000001","title":"새로고침 검증 도서","coverImageUrl":"","reviewCount":4,"replyCount":0}]}
            """
        case "/api/v1/home/latest-reviews":
            let reviews = (1...4).map { index in
                """
                {"reviewId":\(index),"content":"목록의 마지막과 당김 후 기존 내용이 유지되는지 확인하는 감상입니다.","createdAt":"2026-10-01T08:00:0\(index)Z","author":{"displayName":"독자 \(index)"},"replyCount":0,"bookId":1,"isbn13":"9780000000001","bookTitle":"새로고침 검증 도서","bookCoverImageUrl":""}
                """
            }.joined(separator: ",")
            body = "{\"reviews\":[\(reviews)]}"
        case "/api/v1/feed/reviews":
            body = "{\"reviews\":[],\"totalCount\":0,\"nextPage\":null}"
        default:
            body = "{\"totalCount\":0,\"nextPage\":null,\"items\":[]}"
        }
        return Data(body.utf8)
    }
}

/// 신규 인증과 기존 세션 복원을 같은 공개 HTTP 계약으로 검증한다.
private final class NicknameFixtureServer: @unchecked Sendable {
    private let listener: NWListener
    private let queue = DispatchQueue(label: "chaekchaek.nickname-fixture")
    private let lock = NSLock()
    private var writes = 0
    private var nickname = ""
    var nicknameWrites: Int { lock.lock(); defer { lock.unlock() }; return writes }
    var baseURL: String { "http://127.0.0.1:\(listener.port!.rawValue)" }
    init() throws {
        listener = try NWListener(using: .tcp, on: .any)
        let ready = XCTestExpectation(description: "닉네임 API 준비")
        listener.stateUpdateHandler = { if case .ready = $0 { ready.fulfill() } }
        listener.newConnectionHandler = { [weak self] connection in
            guard let self else { return }
            connection.start(queue: self.queue)
            self.receive(connection)
        }
        listener.start(queue: queue)
        guard XCTWaiter.wait(for: [ready], timeout: 5) == .completed else {
            listener.cancel()
            throw NSError(domain: "NicknameFixture", code: 1)
        }
    }
    func reset() { lock.lock(); writes = 0; nickname = ""; lock.unlock() }
    func stop() { listener.cancel() }
    private func receive(_ connection: NWConnection, buffered: Data = Data()) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 65_536) { [weak self] data, _, complete, error in
            guard let self else { return }
            let bytes = buffered + (data ?? Data())
            guard let request = String(data: bytes, encoding: .utf8), request.contains("\r\n\r\n") else {
                if !complete && error == nil { self.receive(connection, buffered: bytes) }
                else { connection.cancel() }
                return
            }
            let path = request.components(separatedBy: " ").dropFirst().first ?? ""
            self.lock.lock()
            var status = "200 OK"
            let body: String
            switch path.components(separatedBy: "?")[0] {
            case "/api/v1/auth/mobile/google", "/api/v1/auth/mobile/reissue":
                body = """
                {"accessToken":"fixture-access","refreshToken":"fixture-refresh","tokenType":"Bearer","accessTokenExpiresIn":3600,"refreshTokenExpiresIn":7200}
                """
            case "/api/v1/members/me/nickname":
                self.writes += 1
                if self.writes == 1 {
                    status = "503 Service Unavailable"
                    body = "{}"
                } else {
                    self.nickname = "reader"
                    body = self.profile()
                }
            case "/api/v1/members/me": body = self.profile()
            case "/api/v1/libraries": body = "{\"memberId\":1,\"libraries\":[]}"
            case "/api/v1/home/popular-books": body = "{\"books\":[]}"
            case "/api/v1/home/latest-reviews": body = "{\"reviews\":[]}"
            case "/api/v1/feed/reviews": body = "{\"reviews\":[],\"totalCount\":0,\"nextPage\":null}"
            default: body = "{\"items\":[],\"totalCount\":0,\"nextPage\":null}"
            }
            self.lock.unlock()
            let payload = Data(body.utf8)
            let header = "HTTP/1.1 \(status)\r\nContent-Type: application/json\r\nContent-Length: \(payload.count)\r\nConnection: close\r\n\r\n"
            connection.send(content: Data(header.utf8) + payload, completion: .contentProcessed { _ in connection.cancel() })
        }
    }
    private func profile() -> String {
        "{\"memberId\":1,\"nickname\":\"\(nickname)\",\"anonymousNickname\":\"참새\",\"displayAnonymous\":true}"
    }
}


final class BookshelfContractUITests: XCTestCase {
    private let origin = "http://127.0.0.1:19068"

    @MainActor
    func testShelfCreateRenameAddRemoveAndImagePreview() async throws {
        let marker = try? await request("/__bookshelf")
        guard marker != nil else { throw XCTSkip("bookshelf-contract fixture가 필요합니다") }
        _ = try await request("/__bookshelf/reset", method: "POST")
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestingExistingInstallation", "-uiTestingGuest", "-uiTestingNicknameRestore", "-uiTestingRefresh"]
        app.launchEnvironment["CHAEKCHAEK_UI_TEST_API_URL"] = origin
        app.launch()
        let shelfTab = app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", "내 서재")).firstMatch
        guard shelfTab.waitForExistence(timeout: 15) else {
            let screen = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            screen.name = "책장 시작 화면 실패"
            screen.lifetime = .keepAlways
            add(screen)
            XCTFail("내 서재 진입 버튼이 표시되지 않았어요: \(app.debugDescription)")
            return
        }
        shelfTab.tap()
        let firstShelf = app.buttons["bookshelf-1"]
        XCTAssertTrue(firstShelf.waitForExistence(timeout: 10))
        app.buttons["bookshelf-menu-1"].tap()
        element("이름 바꾸기", app).tap()
        let input = app.textViews["bookshelfName"]
        XCTAssertTrue(input.waitForExistence(timeout: 5))
        input.tap()
        input.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 20))
        input.typeText("나의 책장")
        element("저장", app).tap()
        XCTAssertTrue(input.waitForNonExistence(timeout: 10))
        XCTAssertTrue(element("나의 책장", app).waitForExistence(timeout: 10))
        app.buttons["bookshelf-create"].tap()
        let newName = app.textViews["bookshelfName"]
        XCTAssertTrue(newName.waitForExistence(timeout: 5))
        newName.tap()
        newName.typeText("올해읽을책")
        element("만들기", app).tap()
        XCTAssertTrue(newName.waitForNonExistence(timeout: 10))
        XCTAssertTrue(firstShelf.waitForExistence(timeout: 10))
        firstShelf.tap()
        let addButton = element("+ 책 담기", app)
        XCTAssertTrue(addButton.waitForExistence(timeout: 10))
        addButton.tap()
        let selectedBook = element("검증 도서 3", app)
        guard selectedBook.waitForExistence(timeout: 10) else {
            XCTFail("내 서재의 책 목록을 찾지 못했어요: \(app.debugDescription)")
            return
        }
        selectedBook.tap()
        XCTAssertTrue(element("담을 책장 고르기", app).waitForExistence(timeout: 5))
        let confirmAdd = element("이 책장에 담기", app)
        for _ in 0..<4 {
            if confirmAdd.exists && confirmAdd.isHittable { break }
            app.swipeUp()
        }
        guard confirmAdd.waitForExistence(timeout: 5) else {
            let screen = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            screen.name = "책장 선택 확인 버튼 실패"
            screen.lifetime = .keepAlways
            add(screen)
            XCTFail("책장 선택을 확인할 수 없어요: \(app.debugDescription)")
            return
        }
        confirmAdd.tap()
        XCTAssertTrue(confirmAdd.waitForNonExistence(timeout: 10))
        XCTAssertTrue(addButton.waitForExistence(timeout: 10))
        element("편집", app).tap()
        let remove = app.buttons.matching(NSPredicate(format: "label == %@", "빼기")).firstMatch
        for _ in 0..<4 {
            if remove.exists && remove.isHittable { break }
            app.swipeUp()
        }
        guard remove.waitForExistence(timeout: 5) else {
            XCTFail("책 빼기 버튼을 찾지 못했어요: \(app.debugDescription)")
            return
        }
        remove.tap()
        element("완료", app).tap()
        let imageShare = element("이미지 공유", app)
        let enabled = NSPredicate(format: "enabled == true")
        let imageReady = expectation(for: enabled, evaluatedWith: imageShare)
        await fulfillment(of: [imageReady], timeout: 15)
        imageShare.tap()
        XCTAssertTrue(element("책장을 이미지로", app).waitForExistence(timeout: 10))
        let saveImage = element("이미지 저장", app)
        XCTAssertTrue(saveImage.exists)
        let saveReady = expectation(for: NSPredicate(format: "enabled == true"), evaluatedWith: saveImage)
        await fulfillment(of: [saveReady], timeout: 15)
        saveImage.tap()
        let permission = XCUIApplication(bundleIdentifier: "com.apple.springboard").alerts.firstMatch
        if permission.waitForExistence(timeout: 5) { permission.buttons.allElementsBoundByIndex.last?.tap() }
        XCTAssertTrue(element("이미지 저장을 완료했어요", app).waitForExistence(timeout: 15))
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "책장 이미지 미리보기"
        attachment.lifetime = .keepAlways
        add(attachment)
        element("이미지 공유", app).tap()
        let copyAction = app.cells.matching(NSPredicate(format: "label == %@ OR label == %@", "복사", "Copy")).firstMatch
        guard copyAction.waitForExistence(timeout: 15) else {
            let failure = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            failure.name = "네이티브 공유창 표시 실패"
            failure.lifetime = .keepAlways
            add(failure)
            XCTFail("네이티브 이미지 공유창이 표시되지 않았어요: \(app.debugDescription)")
            return
        }
        let nativeShare = XCTAttachment(screenshot: app.screenshot())
        nativeShare.name = "네이티브 이미지 공유"
        nativeShare.lifetime = .keepAlways
        add(nativeShare)
        let data = try await request("/__bookshelf/state")
        let state = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        let shelves = try XCTUnwrap(state["shelves"] as? [[String: Any]])
        XCTAssertTrue(shelves.contains { $0["name"] as? String == "올해읽을책" })
        let calls = try XCTUnwrap(state["requests"] as? [[String: Any]])
        XCTAssertTrue(calls.contains { $0["method"] as? String == "POST" && $0["path"] as? String == "/api/v1/libraries/1" })
        XCTAssertTrue(calls.contains { ($0["path"] as? String)?.hasPrefix("/api/v1/books/3/reviews") == true })
        XCTAssertTrue(calls.contains { $0["method"] as? String == "DELETE" && ($0["path"] as? String)?.contains("/books/") == true })
    }

    @MainActor
    private func element(_ text: String, _ app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", text)).firstMatch
    }

    @MainActor
    private func request(_ path: String, method: String = "GET") async throws -> Data {
        var request = URLRequest(url: URL(string: origin + path)!)
        request.httpMethod = method
        request.timeoutInterval = 3
        return try await URLSession.shared.data(for: request).0
    }
}
