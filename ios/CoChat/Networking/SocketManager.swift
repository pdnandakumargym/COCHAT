import Foundation
import SocketIO

enum AppSocketEvent {
    case presenceUpdate(userId: String, status: String)
    case messageNew(message: Message, chatId: String)
    case chatUpdated(chat: ChatSummary)
    case chatRead(chatId: String, userId: String)
    case groupUpdated(chat: GroupInfo, action: String)
    case typingUpdate(chatId: String, userId: String, isTyping: Bool)
    case notificationNew(notification: AppNotification)
}

@MainActor
final class AppSocketManager {
    private var manager: SocketManager?
    private var socket: SocketIOClient?
    private let decoder = JSONDecoder()

    /// Fan-out target; RealtimeStore assigns itself here in `start()`.
    var onEvent: ((AppSocketEvent) -> Void)?

    func connect(token: String) {
        guard socket?.status != .connected else { return }
        let manager = SocketManager(socketURL: NetworkConfig.socketURL, config: [.connectParams(["token": token]), .compress])
        self.manager = manager
        let socket = manager.defaultSocket
        self.socket = socket

        socket.on("presence:update") { [weak self] data, _ in
            guard let dict = data.first as? [String: Any],
                  let userId = dict["userId"] as? String,
                  let status = dict["status"] as? String else { return }
            self?.onEvent?(.presenceUpdate(userId: userId, status: status))
        }
        socket.on("message:new") { [weak self] data, _ in
            guard let self, let dict = data.first as? [String: Any],
                  let chatId = dict["chatId"] as? String,
                  let messageDict = dict["message"],
                  let message = self.decode(Message.self, from: messageDict) else { return }
            self.onEvent?(.messageNew(message: message, chatId: chatId))
        }
        socket.on("chat:updated") { [weak self] data, _ in
            guard let self, let dict = data.first as? [String: Any],
                  let chatDict = dict["chat"],
                  let chat = self.decode(ChatSummary.self, from: chatDict) else { return }
            self.onEvent?(.chatUpdated(chat: chat))
        }
        socket.on("chat:read") { [weak self] data, _ in
            guard let dict = data.first as? [String: Any],
                  let chatId = dict["chatId"] as? String,
                  let userId = dict["userId"] as? String else { return }
            self?.onEvent?(.chatRead(chatId: chatId, userId: userId))
        }
        socket.on("group:updated") { [weak self] data, _ in
            guard let self, let dict = data.first as? [String: Any],
                  let action = dict["action"] as? String,
                  let chatDict = dict["chat"],
                  let group = self.decode(GroupInfo.self, from: chatDict) else { return }
            self.onEvent?(.groupUpdated(chat: group, action: action))
        }
        socket.on("typing:update") { [weak self] data, _ in
            guard let dict = data.first as? [String: Any],
                  let chatId = dict["chatId"] as? String,
                  let userId = dict["userId"] as? String,
                  let isTyping = dict["isTyping"] as? Bool else { return }
            self?.onEvent?(.typingUpdate(chatId: chatId, userId: userId, isTyping: isTyping))
        }
        socket.on("notification:new") { [weak self] data, _ in
            guard let self, let dict = data.first as? [String: Any],
                  let notificationDict = dict["notification"],
                  let notification = self.decode(AppNotification.self, from: notificationDict) else { return }
            self.onEvent?(.notificationNew(notification: notification))
        }

        socket.connect()
    }

    func emitTypingStart(chatId: String) { socket?.emit("typing:start", ["chatId": chatId]) }
    func emitTypingStop(chatId: String) { socket?.emit("typing:stop", ["chatId": chatId]) }
    func emitActivity() { socket?.emit("presence:activity") }

    func disconnect() {
        socket?.disconnect()
        socket = nil
        manager = nil
    }

    private func decode<T: Decodable>(_ type: T.Type, from any: Any) -> T? {
        guard let data = try? JSONSerialization.data(withJSONObject: any) else { return nil }
        return try? decoder.decode(T.self, from: data)
    }
}
