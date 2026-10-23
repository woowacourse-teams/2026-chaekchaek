import Shared
import SwiftUI
import StoreKit
import GoogleSignIn
import FirebaseAnalytics
import PostHog

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
        let showQuestionAPreviewControls: Bool
#if DEBUG
        uiTestingMyPage = launchArguments.contains("-uiTestingMyPage")
        uiTestingBookQuestionA = launchArguments.contains("-uiTestingBookQuestionA")
        showQuestionAPreviewControls = !launchArguments.contains("-uiTestingBookQuestionAContentOnly")
#else
        uiTestingMyPage = false
        uiTestingBookQuestionA = false
        showQuestionAPreviewControls = true
#endif
        let authPlatform = AuthPlatformCallbacks(
            requestGoogleIdToken: { onResult in
                Task { @MainActor in
#if DEBUG
                    if launchArguments.contains("-uiTestingNickname") && launchArguments.contains("-uiTestingRefresh") {
                        _ = onResult("fixture-google", nil)
                        return
                    }
                    if launchArguments.contains("-uiTestingSignInFailure") {
                        _ = onResult(nil, "fixture provider failure")
                        return
                    }
                    if launchArguments.contains("-uiTestingSignInCancellation") {
                        _ = onResult(nil, nil)
                        return
                    }
#endif
                    do {
                        _ = onResult(try await GoogleSignInProvider().signIn(), nil)
                    } catch {
                        let failure = error as NSError
                        let cancelled = failure.domain == kGIDSignInErrorDomain && failure.code == GIDSignInError.canceled.rawValue
                        _ = onResult(nil, cancelled ? nil : error.localizedDescription)
                    }
                }
            },
            readRefreshToken: {
#if DEBUG
                if launchArguments.contains("-uiTestingNicknameRestore") && launchArguments.contains("-uiTestingRefresh") {
                    return "fixture-refresh"
                }
#endif
                return keychain.read()
            },
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
                        _ = onResult(nil, nil)
                    } catch {
                        _ = onResult(nil, error.localizedDescription)
                    }
                }
            },
            readGuest: guestKeychain.read,
            writeGuest: guestKeychain.write,
            clearGuest: guestKeychain.clear,
            readAnonymousReviews: {
                guard UserDefaults.standard.object(forKey: "review_anonymous") != nil else { return nil }
                return KotlinBoolean(bool: UserDefaults.standard.bool(forKey: "review_anonymous"))
            },
            writeAnonymousReviews: { value in
                UserDefaults.standard.set(value.boolValue, forKey: "review_anonymous")
            }
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
        let defaults = UserDefaults.standard
        let samplingSeed = defaults.string(forKey: "analytics_sampling_seed") ?? UUID().uuidString
        defaults.set(samplingSeed, forKey: "analytics_sampling_seed")
        let collection = AnalyticsCollectionController(
            readCachedPolicy: { defaults.string(forKey: "analytics_collection_policy") },
            writeCachedPolicy: { defaults.set($0, forKey: "analytics_collection_policy") },
            samplingSeed: samplingSeed
        )
        return MainViewControllerKt.MainViewController(
            authPlatform: authPlatform,
            recentSearchStorage: recentSearchStorage,
            onboardingStorage: onboardingStorage,
            createGoogleSignInButton: GoogleSignInControl.init,
            analyticsEnvironment: analyticsEnvironment,
            analyticsCollection: collection,
            refreshAnalyticsConfiguration: { PostHogAnalyticsEventLogger.refreshCollectionPolicy(collection) },
            logAnalyticsEvent: AnalyticsEventDispatcher(
                primary: FirebaseAnalyticsEventLogger.log,
                secondary: PostHogAnalyticsEventLogger.log
            ).log,
            logUserError: { PostHogAnalyticsEventLogger.captureUserError($0) },
            uiTestingMyPage: uiTestingMyPage,
            uiTestingBookQuestionA: uiTestingBookQuestionA,
            showQuestionAPreviewControls: showQuestionAPreviewControls
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

enum PostHogAnalyticsEventLogger {
    private static let configured: Bool = {
#if DEBUG
        let isDebug = true
#else
        let isDebug = false
#endif
        guard let config = makeConfiguration(info: Bundle.main.infoDictionary ?? [:], isDebug: isDebug) else { return false }
        PostHogSDK.shared.setup(config)
        return true
    }()

    static func configure() {
        _ = configured
    }

    static func makeConfiguration(info: [String: Any], isDebug: Bool) -> PostHogConfig? {
        if isDebug && info["POSTHOG_CAPTURE_DEBUG"] as? String != "YES" { return nil }
        guard let rawToken = info["POSTHOG_PROJECT_TOKEN"] as? String else { return nil }
        let token = rawToken.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !token.isEmpty, !token.contains("$("),
              let host = info["POSTHOG_HOST"] as? String,
              let url = URL(string: host), url.scheme == "https", url.host != nil else { return nil }
        let config = PostHogConfig(projectToken: token, host: host)
        config.enableSwizzling = false
        config.captureApplicationLifecycleEvents = false
        config.captureScreenViews = false
        config.captureElementInteractions = false
        config.captureSwiftUIElementInteractions = false
        config.capturePushNotificationSubscriptions = false
        config.capturePushNotificationOpened = false
        config.preloadFeatureFlags = false
        config.sendFeatureFlagEvent = false
        config.personProfiles = .never
        config.sessionReplay = false
        config.surveys = false
        config.errorTrackingConfig.autoCapture = true
        config.errorTrackingConfig.exceptionSteps.enabled = false
        config.errorTrackingConfig.inAppIncludes.append("Shared")
        config.setBeforeSend { @Sendable event in
            guard event.event == "$exception" else { return event }
            guard let data = try? JSONSerialization.data(withJSONObject: event.properties),
                  let json = String(data: data, encoding: .utf8),
                  let clean = UserErrorPrivacyKt.sanitizeExceptionProperties(propertiesJson: json),
                  let cleanData = clean.data(using: .utf8),
                  let properties = try? JSONSerialization.jsonObject(with: cleanData) as? [String: Any] else { return nil }
            event.properties = properties
            return event
        }
        return config
    }

    @MainActor
    static func refreshCollectionPolicy(_ controller: AnalyticsCollectionController) {
        guard configured else { return }
        let receiver = AnalyticsPolicyReceiver(controller)
        PostHogSDK.shared.reloadFeatureFlags { @Sendable in
            guard let payload = PostHogSDK.shared.getFeatureFlagPayload(AnalyticsCollectionController.companion.FlagKey) as? [String: Any],
                  let data = try? JSONSerialization.data(withJSONObject: payload),
                  let json = String(data: data, encoding: .utf8) else { return }
            DispatchQueue.main.async { receiver.receive(json) }
        }
    }

    static func captureUserError(_ propertiesJson: String) {
        guard configured,
              let data = propertiesJson.data(using: .utf8),
              let properties = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }
        PostHogSDK.shared.capture("$exception", properties: properties)
    }

    static func log(event: AnalyticsEvent) {
        guard configured else { return }
        let strings = event.stringParameters
        var crashContext: [String: Any] = [:]
        if let run = strings["run_id"] { crashContext["cc_run_id"] = run }
        if let screen = strings["screen_name"] { crashContext["cc_screen_name"] = screen }
        if !["cc_app_lifecycle", "cc_view_engagement", "cc_content_impression"].contains(event.name) {
            crashContext["cc_last_event"] = event.name
            if let action = strings["action"] { crashContext["cc_last_action"] = action }
        }
        PostHogSDK.shared.register(crashContext)
        var properties = analyticsParameters(event)
        properties["$geoip_disable"] = true
        PostHogSDK.shared.capture(event.name, properties: properties)
    }
}

// SDK의 완료 콜백에서 받은 정책을 공통 계측과 같은 main actor에서 적용한다.
@MainActor
private final class AnalyticsPolicyReceiver {
    private let controller: AnalyticsCollectionController

    init(_ controller: AnalyticsCollectionController) {
        self.controller = controller
    }

    func receive(_ payload: String) {
        _ = controller.receive(payload: payload)
    }
}

private enum FirebaseAnalyticsEventLogger {
    static func log(event: AnalyticsEvent) {
        Analytics.logEvent(event.name, parameters: analyticsParameters(FirebaseAnalyticsParametersKt.firebaseAnalyticsEvent(event: event)))
    }
}

func analyticsParameters(_ event: AnalyticsEvent) -> [String: Any] {
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
    return parameters
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

        isOpaque = false
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
        UIColor.white.setFill()
        UIBezierPath(rect: bounds).fill()
        let foregroundAlpha: CGFloat = isEnabled && !isHighlighted ? 1 : 0.5
        let title = "Google로 계속하기"
        let attributes: [NSAttributedString.Key: Any] = [
            .font: providerFont,
            .foregroundColor: UIColor(red: 23.0 / 255.0, green: 23.0 / 255.0, blue: 23.0 / 255.0, alpha: foregroundAlpha),
        ]
        let textSize = title.size(withAttributes: attributes)
        let logoHeight = 18.0
        let logoWidth = logoHeight * logo.size.width / logo.size.height
        let contentWidth = logoWidth + 12 + textSize.width
        let contentX = (bounds.width - contentWidth) / 2

        logo.draw(in: CGRect(x: contentX, y: (bounds.height - logoHeight) / 2, width: logoWidth, height: logoHeight), blendMode: .normal, alpha: foregroundAlpha)
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
        setNeedsDisplay()
    }
}
