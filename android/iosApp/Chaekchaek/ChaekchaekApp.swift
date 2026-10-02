import SwiftUI
import GoogleSignIn
import FirebaseCore

@main
struct ChaekchaekApp: App {
    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
    }
}
