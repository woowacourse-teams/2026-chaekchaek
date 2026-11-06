import XCTest

final class PmfSurveyUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    @MainActor func testSelectionsClosePreviousAndRetry() throws {
        let app = launch()
        let bar = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "책췍 설문 참여하기")).firstMatch
        XCTAssertTrue(bar.waitForExistence(timeout: 10))
        capture("설문 바")
        bar.tap()
        XCTAssertFalse(button("다음", app).isEnabled)
        button("매우 실망", app).tap()
        capture("설문 첫 문항")
        button("다음", app).tap()
        button("내 서재와 책장", app).tap()
        button("이전", app).tap()
        XCTAssertTrue(button("다음", app).isEnabled)
        button("닫기", app).tap()
        bar.tap()
        XCTAssertTrue(button("다음", app).isEnabled)
        button("다음", app).tap()
        XCTAssertTrue(button("다음", app).isEnabled)
        button("다음", app).tap()
        button("다른 사람들 감상 보기(피드)", app).tap()
        button("책 검색하기", app).tap()
        let none = button("몰랐던 기능 없음", app)
        if !none.isHittable { app.swipeUp() }
        none.tap()
        capture("설문 복수 선택")
        button("다음", app).tap()
        button("예", app).tap()
        button("제출하기", app).tap()
        XCTAssertTrue(app.staticTexts["설문을 제출하지 못했어요"].waitForExistence(timeout: 5))
        capture("설문 제출 실패")
        button("다시 시도", app).tap()
        XCTAssertTrue(app.staticTexts["의견을 들려주셔서 고마워요"].waitForExistence(timeout: 5))
        capture("설문 완료")
        button("확인", app).tap()
        XCTAssertFalse(bar.exists)
    }

    @MainActor func testSwipeDismissesWithoutOpeningSurvey() throws {
        let app = launch()
        let bar = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "책췍 설문 참여하기")).firstMatch
        XCTAssertTrue(bar.waitForExistence(timeout: 10))
        bar.swipeLeft()
        XCTAssertFalse(bar.waitForExistence(timeout: 2))
        XCTAssertFalse(app.staticTexts["이 제품을 더 이상 쓸 수 없다면 어떻겠습니까?"].exists)
        capture("설문 바 스와이프 숨김")
    }

    @MainActor private func launch() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestingPmfSurvey", "-uiTestingExistingInstallation"]
        app.launch()
        return app
    }
    @MainActor private func button(_ label: String, _ app: XCUIApplication) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label == %@ OR label BEGINSWITH %@", label, label + ",")).firstMatch
    }
    @MainActor private func capture(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name; attachment.lifetime = .keepAlways; add(attachment)
    }
}
