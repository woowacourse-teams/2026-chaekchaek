import XCTest
import Shared
@testable import Chaekchaek

final class ChaekchaekTests: XCTestCase {
    @MainActor
    func testCommonComposeEntryPointCanBeCreated() {
        _ = ContentView()
    }

    @MainActor
    func testAppleNonceUsesBase64UrlFormat() throws {
        let nonce = try AppleSignInProvider.makeNonce()

        XCTAssertEqual(nonce.count, 43)
        XCTAssertNotNil(nonce.range(of: "^[A-Za-z0-9_-]{43}$", options: .regularExpression))
    }

    @MainActor
    func testAppleNonceSha256MatchesKnownValue() {
        XCTAssertEqual(
            AppleSignInProvider.sha256("abc"),
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        )
    }

    func testPostHogRemainsDisabledWithoutProjectToken() {
        XCTAssertNil(PostHogAnalyticsEventLogger.makeConfiguration(info: [:], isDebug: false))
        XCTAssertNil(PostHogAnalyticsEventLogger.makeConfiguration(
            info: ["POSTHOG_PROJECT_TOKEN": "$(POSTHOG_PROJECT_TOKEN)", "POSTHOG_HOST": "https://us.i.posthog.com"],
            isDebug: false
        ))
    }

    func testPostHogDebugCaptureRequiresExplicitOptIn() {
        var info: [String: Any] = [
            "POSTHOG_PROJECT_TOKEN": "test-token",
            "POSTHOG_HOST": "https://us.i.posthog.com"
        ]
        XCTAssertNil(PostHogAnalyticsEventLogger.makeConfiguration(info: info, isDebug: true))
        info["POSTHOG_CAPTURE_DEBUG"] = "YES"
        XCTAssertNotNil(PostHogAnalyticsEventLogger.makeConfiguration(info: info, isDebug: true))
    }

    func testPostHogOnlyCapturesExistingManualEvents() throws {
        let config = try XCTUnwrap(PostHogAnalyticsEventLogger.makeConfiguration(
            info: ["POSTHOG_PROJECT_TOKEN": "test-token", "POSTHOG_HOST": "https://us.i.posthog.com"],
            isDebug: false
        ))
        XCTAssertFalse(config.captureApplicationLifecycleEvents)
        XCTAssertFalse(config.captureScreenViews)
        XCTAssertFalse(config.captureElementInteractions)
        XCTAssertFalse(config.captureSwiftUIElementInteractions)
        XCTAssertFalse(config.preloadFeatureFlags)
        XCTAssertFalse(config.sessionReplay)
        XCTAssertFalse(config.surveys)
        XCTAssertFalse(config.enableSwizzling)
    }

    func testPostHogRejectsInsecureHost() {
        XCTAssertNil(PostHogAnalyticsEventLogger.makeConfiguration(
            info: ["POSTHOG_PROJECT_TOKEN": "test-token", "POSTHOG_HOST": "http://us.i.posthog.com"],
            isDebug: false
        ))
    }
}
