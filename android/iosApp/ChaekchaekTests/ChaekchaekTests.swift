import XCTest
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
}
