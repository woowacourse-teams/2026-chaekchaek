import Shared
import SwiftUI

struct ContentView: View {
    var body: some View {
        ComposeViewController()
            .ignoresSafeArea()
    }
}

private struct ComposeViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let keychain = RefreshTokenKeychain()
        let authPlatform = AuthPlatformCallbacks(
            requestGoogleIdToken: { onResult in
                Task { @MainActor in
                    do {
                        _ = onResult(try await GoogleSignInProvider().signIn(), nil)
                    } catch {
                        _ = onResult(nil, error.localizedDescription)
                    }
                }
            },
            readRefreshToken: keychain.read,
            writeRefreshToken: keychain.write,
            clearRefreshToken: keychain.clear
        )
        return MainViewControllerKt.MainViewController(authPlatform: authPlatform)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
