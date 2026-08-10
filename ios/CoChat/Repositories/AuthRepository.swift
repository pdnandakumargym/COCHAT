import Foundation

final class AuthRepository {
    private let api: APIClient
    private let tokenStore: TokenStore

    init(api: APIClient, tokenStore: TokenStore) {
        self.api = api
        self.tokenStore = tokenStore
    }

    var isLoggedIn: Bool { tokenStore.isLoggedIn }

    func register(fullName: String, email: String?, mobile: String?, password: String, designation: String?) async throws {
        let body = RegisterRequest(fullName: fullName, email: email, mobile: mobile, password: password, designation: designation)
        let res: AuthResponse = try await api.post("auth/register", body: body)
        tokenStore.save(accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user)
    }

    func login(identifier: String, password: String) async throws {
        let res: AuthResponse = try await api.post("auth/login", body: LoginRequest(identifier: identifier, password: password))
        tokenStore.save(accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user)
    }

    func logout() async {
        if let refreshToken = tokenStore.refreshToken {
            try? await api.postVoid("auth/logout", body: RefreshRequest(refreshToken: refreshToken))
        }
        tokenStore.clear()
    }
}
