import Foundation

/// Swift tuples aren't Equatable even when every element is, and SwiftUI's
/// `.onChange(of:)` requires Equatable — so these are plain structs, not
/// tuples, purely so views can observe them.
struct IncomingMessage: Equatable {
    let chatId: String
    let message: Message
}

struct GroupUpdate: Equatable {
    let chat: GroupInfo
    let action: String
}

@MainActor
final class RealtimeStore: ObservableObject {
    @Published private(set) var chats: [ChatSummary] = []
    @Published private(set) var presence: [String: String] = [:]
    @Published var notifications: [AppNotification] = []
    @Published private(set) var typing: [String: Set<String>] = [:]

    /// Latest live message per chat, for open threads to append without a refetch.
    @Published private(set) var lastIncomingMessage: IncomingMessage?
    @Published private(set) var lastGroupUpdate: GroupUpdate?

    var unreadNotificationCount: Int { notifications.filter { !$0.read }.count }
    var totalUnreadChats: Int { chats.reduce(0) { $0 + $1.unreadCount } }

    private let socket: AppSocketManager
    private let chatsRepo: ChatsRepository
    private let notificationsRepo: NotificationsRepository
    private let tokenStore: TokenStore
    private var started = false

    init(socket: AppSocketManager, chatsRepo: ChatsRepository, notificationsRepo: NotificationsRepository, tokenStore: TokenStore) {
        self.socket = socket
        self.chatsRepo = chatsRepo
        self.notificationsRepo = notificationsRepo
        self.tokenStore = tokenStore
    }

    func start() async {
        guard !started, let token = tokenStore.accessToken else { return }
        started = true

        // Assigned before connect() — the callback closure is live the instant
        // it's set, so the server's own presence:update on connect can't race
        // ahead of a listener the way an async Flow subscription could.
        socket.onEvent = { [weak self] event in
            Task { @MainActor in self?.handle(event) }
        }
        socket.connect(token: token)

        async let fetchedChats = chatsRepo.list()
        async let fetchedNotifications = notificationsRepo.list()
        if let chatsResult = try? await fetchedChats { chats = chatsResult }
        if let notificationsResult = try? await fetchedNotifications { notifications = notificationsResult }

        var snapshot: [String: String] = [:]
        for chat in chats {
            if let peer = chat.peer { snapshot[peer.id] = peer.status }
        }
        presence = snapshot.merging(presence) { _, live in live }
    }

    func stop() {
        socket.disconnect()
        started = false
        chats = []
        presence = [:]
        notifications = []
    }

    func emitTypingStart(chatId: String) { socket.emitTypingStart(chatId: chatId) }
    func emitTypingStop(chatId: String) { socket.emitTypingStop(chatId: chatId) }

    func markChatReadLocally(_ chatId: String) {
        chats = chats.map { chat in
            var updated = chat
            if updated.id == chatId { updated.unreadCount = 0 }
            return updated
        }
    }

    /// Inserts a freshly-created chat (e.g. from "Message" on a team member)
    /// so it's visible immediately, without waiting on a socket event or refetch.
    func upsertChat(_ chat: ChatSummary) {
        if let idx = chats.firstIndex(where: { $0.id == chat.id }) {
            chats[idx] = chat
        } else {
            chats.insert(chat, at: 0)
        }
    }

    func replaceChatsForNewGroup() {
        Task {
            if let list = try? await chatsRepo.list() { chats = list }
        }
    }

    private func handle(_ event: AppSocketEvent) {
        switch event {
        case .presenceUpdate(let userId, let status):
            presence[userId] = status
        case .messageNew(let message, let chatId):
            lastIncomingMessage = IncomingMessage(chatId: chatId, message: message)
        case .chatUpdated(let chat):
            if let idx = chats.firstIndex(where: { $0.id == chat.id }) {
                chats[idx] = chat
            } else {
                chats.insert(chat, at: 0)
            }
            chats.sort { $0.updatedAt > $1.updatedAt }
        case .groupUpdated(let chat, let action):
            lastGroupUpdate = GroupUpdate(chat: chat, action: action)
            if action == "created" { replaceChatsForNewGroup() }
        case .notificationNew(let notification):
            notifications.insert(notification, at: 0)
        case .typingUpdate(let chatId, let userId, let isTyping):
            var set = typing[chatId] ?? []
            if isTyping { set.insert(userId) } else { set.remove(userId) }
            typing[chatId] = set
        case .chatRead:
            break
        }
    }
}
