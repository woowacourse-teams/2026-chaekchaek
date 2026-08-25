import GoogleSignIn
import UIKit

@MainActor
struct GoogleSignInProvider {
    enum SignInError: LocalizedError {
        case missingConfiguration
        case missingPresenter
        case missingIDToken

        var errorDescription: String? {
            switch self {
            case .missingConfiguration:
                "Google 로그인 설정이 필요해요."
            case .missingPresenter:
                "로그인 화면을 열지 못했어요."
            case .missingIDToken:
                "Google 인증 정보를 받지 못했어요."
            }
        }
    }

    func signIn() async throws -> String {
        guard let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String,
              !clientID.isEmpty,
              !clientID.contains("$(") else {
            throw SignInError.missingConfiguration
        }
        guard let presenter = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController else {
            throw SignInError.missingPresenter
        }

        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
        guard let idToken = result.user.idToken?.tokenString, !idToken.isEmpty else {
            throw SignInError.missingIDToken
        }
        return idToken
    }
}
