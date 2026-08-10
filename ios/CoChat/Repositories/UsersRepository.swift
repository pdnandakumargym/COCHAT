import Foundation

final class UsersRepository {
    private let api: APIClient
    private let tokenStore: TokenStore

    init(api: APIClient, tokenStore: TokenStore) {
        self.api = api
        self.tokenStore = tokenStore
    }

    func me() async throws -> User {
        let res: UserEnvelope = try await api.get("users/me")
        return res.user
    }

    func updateProfile(fullName: String?, designation: String?) async throws -> User {
        let res: UserEnvelope = try await api.patch("users/me", body: UpdateProfileRequest(fullName: fullName, designation: designation))
        tokenStore.updateUser(res.user)
        return res.user
    }

    func uploadAvatar(data: Data, fileName: String, mimeType: String) async throws -> User {
        let res: UserEnvelope = try await api.postMultipart(
            "users/me/avatar",
            fileField: "avatar", fileData: data, fileName: fileName, mimeType: mimeType
        )
        tokenStore.updateUser(res.user)
        return res.user
    }

    func list(query: String = "") async throws -> [User] {
        let res: UsersEnvelope = try await api.get("users", query: ["q": query])
        return res.users
    }

    func get(id: String) async throws -> User {
        let res: UserEnvelope = try await api.get("users/\(id)")
        return res.user
    }
}
