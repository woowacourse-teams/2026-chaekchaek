import Shared
import SwiftUI
import GoogleSignIn

struct ContentView: View {
    var body: some View {
        ComposeViewController()
            .ignoresSafeArea()
    }
}

private struct ComposeViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let keychain = RefreshTokenKeychain()
#if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-uiTestingGuest") {
            keychain.clear()
        }
#endif
        let appleSignIn = AppleSignInProvider()
        let uiTestingMyPage: Bool
#if DEBUG
        uiTestingMyPage = ProcessInfo.processInfo.arguments.contains("-uiTestingMyPage")
#else
        uiTestingMyPage = false
#endif
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
            clearRefreshToken: keychain.clear,
            requestAppleCredential: { onResult in
                Task { @MainActor in
                    do {
                        let credential = try await appleSignIn.signIn()
                        _ = onResult(
                            AppleSignInCredential(
                                identityToken: credential.identityToken,
                                authorizationCode: credential.authorizationCode,
                                nonce: credential.nonce
                            ),
                            nil
                        )
                    } catch is CancellationError {
                        _ = onResult(nil, "Apple 로그인을 취소했어요.")
                    } catch {
                        _ = onResult(nil, error.localizedDescription)
                    }
                }
            },
            readGuest: { nil },
            writeGuest: { _ in },
            clearGuest: {}
        )
        return MainViewControllerKt.MainViewController(
            authPlatform: authPlatform,
            createGoogleSignInButton: {
                let button = GIDSignInButton()
                button.style = .wide
                button.colorScheme = .light
                return button
            },
            uiTestingMyPage: uiTestingMyPage
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
