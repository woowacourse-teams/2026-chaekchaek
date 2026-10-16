import Foundation
import Security
import Shared

final class RefreshTokenKeychain {
    private let store = KeychainStringStore(account: "refresh_token")

    func read() -> String? {
        store.read()
    }

    func write(_ refreshToken: String) {
        store.write(refreshToken)
    }

    func clear() {
        store.clear()
    }
}

final class GuestAuthKeychain {
    private let tokenStore = KeychainStringStore(account: "guest_token")
    private let nicknameStore = KeychainStringStore(account: "guest_nickname")
    private let expiresAtStore = KeychainStringStore(account: "guest_expires_at")

    func read() -> GuestAuth? {
        guard let token = tokenStore.read(),
              let nickname = nicknameStore.read(),
              let expiresAt = expiresAtStore.read() else {
            clear()
            return nil
        }
        return GuestAuth(token: token, nickname: nickname, expiresAt: expiresAt)
    }

    func write(_ guest: GuestAuth) {
        tokenStore.write(guest.token)
        nicknameStore.write(guest.nickname)
        expiresAtStore.write(guest.expiresAt)
    }

    func clear() {
        tokenStore.clear()
        nicknameStore.clear()
        expiresAtStore.clear()
    }
}

private final class KeychainStringStore {
    private let service = Bundle.main.bundleIdentifier ?? "com.chamsae.chaekchaek"
    private let account: String

    init(account: String) {
        self.account = account
    }

    func read() -> String? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    func write(_ value: String) {
        let data = Data(value.utf8)
        let attributes = [kSecValueData as String: data]
        let status = SecItemUpdate(baseQuery as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var item = baseQuery
            item[kSecValueData as String] = data
            item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            SecItemAdd(item as CFDictionary, nil)
        }
    }

    func clear() {
        SecItemDelete(baseQuery as CFDictionary)
    }

    private var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }
}
