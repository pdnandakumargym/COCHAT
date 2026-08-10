import Foundation
import Security

/// Minimal Keychain-backed JWT/user storage. Access token, refresh token, and
/// the last-known user profile are stored as separate generic-password items
/// under this app's Keychain access group (default: the app's own).
final class TokenStore: ObservableObject {
    private let service = "com.cochat.app.session"
    @Published private(set) var currentUser: User?

    init() {
        currentUser = readUser()
    }

    var accessToken: String? { read("accessToken") }
    var refreshToken: String? { read("refreshToken") }
    var isLoggedIn: Bool { accessToken != nil }

    func save(accessToken: String, refreshToken: String, user: User) {
        write("accessToken", accessToken)
        write("refreshToken", refreshToken)
        if let data = try? JSONEncoder().encode(user) {
            write("user", String(data: data, encoding: .utf8) ?? "")
        }
        DispatchQueue.main.async { self.currentUser = user }
    }

    func updateUser(_ user: User) {
        if let data = try? JSONEncoder().encode(user) {
            write("user", String(data: data, encoding: .utf8) ?? "")
        }
        DispatchQueue.main.async { self.currentUser = user }
    }

    func clear() {
        for key in ["accessToken", "refreshToken", "user"] { delete(key) }
        DispatchQueue.main.async { self.currentUser = nil }
    }

    private func readUser() -> User? {
        guard let json = read("user"), let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(User.self, from: data)
    }

    private func read(_ key: String) -> String? {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func write(_ key: String, _ value: String) {
        let data = Data(value.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        SecItemDelete(query as CFDictionary)
        var attributes = query
        attributes[kSecValueData as String] = data
        SecItemAdd(attributes as CFDictionary, nil)
    }

    private func delete(_ key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        SecItemDelete(query as CFDictionary)
    }
}
