import AuthenticationServices
import CryptoKit
import Foundation
import Security
import UIKit

@MainActor
final class AppleSignInProvider: NSObject {
    struct Credential {
        let identityToken: String
        let authorizationCode: String
        let nonce: String
    }

    enum SignInError: LocalizedError {
        case alreadySigningIn
        case invalidCredential
        case missingPresenter
        case randomGenerationFailed

        var errorDescription: String? {
            switch self {
            case .alreadySigningIn:
                "Apple 로그인이 이미 진행 중이에요."
            case .invalidCredential:
                "Apple 인증 정보를 받지 못했어요."
            case .missingPresenter:
                "로그인 화면을 열지 못했어요."
            case .randomGenerationFailed:
                "Apple 로그인을 시작하지 못했어요."
            }
        }
    }

    private var authorizationController: ASAuthorizationController?
    private var continuation: CheckedContinuation<Credential, Error>?
    private var nonce: String?
    private var presentationWindow: UIWindow?

    func signIn() async throws -> Credential {
        guard continuation == nil else { throw SignInError.alreadySigningIn }
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows)
            .first(where: \.isKeyWindow) else {
            throw SignInError.missingPresenter
        }

        let nonce = try Self.makeNonce()
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        authorizationController = controller
        presentationWindow = window
        self.nonce = nonce

        return try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation
            controller.performRequests()
        }
    }

    private func finish(_ result: Result<Credential, Error>) {
        continuation?.resume(with: result)
        continuation = nil
        authorizationController = nil
        nonce = nil
        presentationWindow = nil
    }

    private static func makeNonce() throws -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        let status = bytes.withUnsafeMutableBytes { buffer in
            SecRandomCopyBytes(kSecRandomDefault, buffer.count, buffer.baseAddress!)
        }
        guard status == errSecSuccess else { throw SignInError.randomGenerationFailed }
        return Data(bytes).base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    private static func sha256(_ value: String) -> String {
        SHA256.hash(data: Data(value.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}

extension AppleSignInProvider: ASAuthorizationControllerDelegate {
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard let appleCredential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let identityTokenData = appleCredential.identityToken,
              let authorizationCodeData = appleCredential.authorizationCode,
              let identityToken = String(data: identityTokenData, encoding: .utf8),
              let authorizationCode = String(data: authorizationCodeData, encoding: .utf8),
              let nonce else {
            finish(.failure(SignInError.invalidCredential))
            return
        }
        finish(.success(Credential(
            identityToken: identityToken,
            authorizationCode: authorizationCode,
            nonce: nonce
        )))
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        let nsError = error as NSError
        if nsError.domain == ASAuthorizationError.errorDomain,
           nsError.code == ASAuthorizationError.canceled.rawValue {
            finish(.failure(CancellationError()))
        } else {
            finish(.failure(error))
        }
    }
}

extension AppleSignInProvider: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        presentationWindow!
    }
}
