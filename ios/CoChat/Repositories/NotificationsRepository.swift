import Foundation

final class NotificationsRepository {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func list() async throws -> [AppNotification] {
        let res: NotificationsEnvelope = try await api.get("notifications")
        return res.notifications
    }

    func markRead(id: String) async throws {
        try await api.postVoid("notifications/\(id)/read")
    }

    func markAllRead() async throws {
        try await api.postVoid("notifications/read-all")
    }
}
