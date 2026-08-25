import SwiftUI
import GoogleSignIn

@main
struct ChaekchaekApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
    }
}
