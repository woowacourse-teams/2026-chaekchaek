import XCTest
@testable import Chaekchaek

final class ChaekchaekTests: XCTestCase {
    @MainActor
    func testCommonComposeEntryPointCanBeCreated() {
        _ = ContentView()
    }
}
