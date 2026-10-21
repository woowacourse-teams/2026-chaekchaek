import Shared
import SwiftUI
import StoreKit
import GoogleSignIn
import FirebaseAnalytics

struct ContentView: View {
    @State private var onboardingEligible: Bool?

    var body: some View {
        Group {
            if let onboardingEligible {
                ComposeViewController(onboardingEligible: onboardingEligible)
            } else {
                Color.white
            }
        }
        .ignoresSafeArea()
        .task {
            if onboardingEligible == nil {
                onboardingEligible = await FirstLibraryInstallationPolicy.isEligible()
            }
        }
    }
}

private enum FirstLibraryInstallationPolicy {
    static let shownKey = "first_library_onboarding_shown"
    static let eligibleKey = "first_library_onboarding_eligible"

    @MainActor
    static func isEligible() async -> Bool {
        let defaults = UserDefaults.standard
#if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("-uiTestingResetOnboarding") {
            defaults.removeObject(forKey: shownKey)
            defaults.removeObject(forKey: eligibleKey)
            return true
        }
        if arguments.contains("-uiTestingExistingInstallation") { return false }
#endif
        if defaults.bool(forKey: shownKey) { return false }
        if let eligible = defaults.object(forKey: eligibleKey) as? Bool { return eligible }
        do {
            guard case .verified(let transaction) = try await AppTransaction.shared,
                  transaction.environment == .production,
                  let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String,
                  transaction.appVersion == build else { return false }
            let eligible = transaction.originalAppVersion == transaction.appVersion
            defaults.set(eligible, forKey: eligibleKey)
            return eligible
        } catch {
            // 판별 불가 상태는 저장하지 않고 다음 실행에서 다시 확인한다.
            return false
        }
    }
}

private struct ComposeViewController: UIViewControllerRepresentable {
    let onboardingEligible: Bool
    func makeUIViewController(context: Context) -> UIViewController {
        let keychain = RefreshTokenKeychain()
        let guestKeychain = GuestAuthKeychain()
#if DEBUG
        let launchArguments = ProcessInfo.processInfo.arguments
        if launchArguments.contains("-uiTestingResetOnboarding") {
            UserDefaults.standard.removeObject(forKey: "first_library_onboarding_shown")
        } else if launchArguments.contains("-uiTestingGuest") || launchArguments.contains("-uiTestingMyPage") {
            UserDefaults.standard.set(true, forKey: "first_library_onboarding_shown")
        }
        if launchArguments.contains("-uiTestingGuest") {
            keychain.clear()
            guestKeychain.clear()
        }
#endif
        let appleSignIn = AppleSignInProvider()
        let uiTestingMyPage: Bool
        let uiTestingBookQuestionA: Bool
#if DEBUG
        uiTestingMyPage = launchArguments.contains("-uiTestingMyPage")
        uiTestingBookQuestionA = launchArguments.contains("-uiTestingBookQuestionA")
#else
        uiTestingMyPage = false
        uiTestingBookQuestionA = false
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
            readGuest: guestKeychain.read,
            writeGuest: guestKeychain.write,
            clearGuest: guestKeychain.clear
        )
        let recentSearchStorage = RecentSearchStorage(
            read: {
                UserDefaults.standard.stringArray(forKey: "recent_searches") ?? []
            },
            write: { queries in
                UserDefaults.standard.set(queries, forKey: "recent_searches")
            }
        )
        let onboardingStorage = OnboardingStorage(
            hasBeenShown: {
                KotlinBoolean(bool: !onboardingEligible || UserDefaults.standard.bool(forKey: "first_library_onboarding_shown"))
            },
            markShown: {
                UserDefaults.standard.set(true, forKey: "first_library_onboarding_shown")
            }
        )
        return MainViewControllerKt.MainViewController(
            authPlatform: authPlatform,
            recentSearchStorage: recentSearchStorage,
            onboardingStorage: onboardingStorage,
            createGoogleSignInButton: GoogleSignInControl.init,
            analyticsEnvironment: analyticsEnvironment,
            logAnalyticsEvent: FirebaseAnalyticsEventLogger.log,
            uiTestingMyPage: uiTestingMyPage,
            uiTestingBookQuestionA: uiTestingBookQuestionA,
            showQuestionAPreviewControls: !launchArguments.contains("-uiTestingBookQuestionAContentOnly")
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private var analyticsEnvironment: String {
#if DEBUG
    "test"
#else
    "production"
#endif
}

private enum FirebaseAnalyticsEventLogger {
    static func log(event: AnalyticsEvent) {
        var parameters: [String: Any] = [:]
        (event.stringParameters as NSDictionary).forEach { key, value in
            if let key = key as? String { parameters[key] = value }
        }
        (event.longParameters as NSDictionary).forEach { key, value in
            if let key = key as? String { parameters[key] = value }
        }
        (event.doubleParameters as NSDictionary).forEach { key, value in
            if let key = key as? String { parameters[key] = value }
        }
        Analytics.logEvent(event.name, parameters: parameters)
    }
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
