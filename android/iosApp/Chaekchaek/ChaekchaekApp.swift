import SwiftUI
import GoogleSignIn
import FirebaseCore
import FirebaseAnalytics

@main
struct ChaekchaekApp: App {
    init() {
        FirebaseApp.configure()
#if DEBUG
        Analytics.setAnalyticsCollectionEnabled(false)
#else
        Analytics.setAnalyticsCollectionEnabled(true)
#endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
    }
}
