import Foundation

final class ChatsRepository {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func list() async throws -> [ChatSummary] {
        let res: ChatsEnvelope = try await api.get("chats")
        return res.chats
    }

    func openPrivate(userId: String) async throws -> ChatSummary {
        let res: ChatEnvelope = try await api.post("chats/private", body: OpenPrivateChatRequest(userId: userId))
        return res.chat
    }

    func messages(chatId: String, before: String? = nil) async throws -> [Message] {
        var query: [String: String] = [:]
        if let before { query["before"] = before }
        let res: MessagesEnvelope = try await api.get("chats/\(chatId)/messages", query: query)
        return res.messages
    }

    func sendMessage(chatId: String, text: String?, fileData: Data?, fileName: String?, mimeType: String?) async throws -> Message {
        var fields: [String: String] = [:]
        if let text, !text.isEmpty { fields["text"] = text }
        let res: MessageEnvelope = try await api.postMultipart(
            "chats/\(chatId)/messages",
            fields: fields,
            fileField: fileData != nil ? "file" : nil,
            fileData: fileData, fileName: fileName, mimeType: mimeType
        )
        return res.message
    }

    func markRead(chatId: String) async throws {
        try await api.postVoid("chats/\(chatId)/read")
    }
}
