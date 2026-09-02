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
            createGoogleSignInButton: GoogleSignInControl.init,
            uiTestingMyPage: uiTestingMyPage
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private final class GoogleSignInControl: UIControl {
    private let logo: UIImage
    private let providerFont: UIFont

    override var isEnabled: Bool {
        didSet { updateAlpha() }
    }

    override var isHighlighted: Bool {
        didSet { updateAlpha() }
    }

    init() {
        _ = GIDSignInButton()
        logo = UIImage(named: "GoogleSignInLogo") ?? UIImage()
        providerFont = UIFont(name: "Roboto-Bold", size: 17) ?? .systemFont(ofSize: 17, weight: .semibold)
        super.init(frame: .zero)

        backgroundColor = .white
        clipsToBounds = true
        layer.cornerRadius = 8
        layer.borderColor = UIColor(red: 142.0 / 255.0, green: 145.0 / 255.0, blue: 143.0 / 255.0, alpha: 1).cgColor
        layer.borderWidth = 1

        isAccessibilityElement = true
        accessibilityIdentifier = "GIDSignInButton"
        accessibilityLabel = "Google로 계속하기"
        accessibilityTraits = .button
    }

    override func draw(_ rect: CGRect) {
        let title = "Google로 계속하기"
        let attributes: [NSAttributedString.Key: Any] = [
            .font: providerFont,
            .foregroundColor: UIColor(red: 23.0 / 255.0, green: 23.0 / 255.0, blue: 23.0 / 255.0, alpha: 1),
        ]
        let textSize = title.size(withAttributes: attributes)
        let logoHeight = 18.0
        let logoWidth = logoHeight * logo.size.width / logo.size.height
        let contentWidth = logoWidth + 12 + textSize.width
        let contentX = (bounds.width - contentWidth) / 2

        logo.draw(in: CGRect(x: contentX, y: (bounds.height - logoHeight) / 2, width: logoWidth, height: logoHeight))
        title.draw(
            at: CGPoint(x: contentX + logoWidth + 12, y: (bounds.height - textSize.height) / 2),
            withAttributes: attributes
        )
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func updateAlpha() {
        alpha = isEnabled && !isHighlighted ? 1 : 0.5
    }
}
