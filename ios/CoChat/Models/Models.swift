import Foundation

enum PresenceStatus: String, Codable {
    case online, away, offline
}

struct User: Codable, Identifiable, Equatable {
    let id: String
    var fullName: String
    var email: String?
    var mobile: String?
    var designation: String
    var profilePicture: String
    var status: String
    var lastSeen: String?
}

struct AuthResponse: Codable {
    let user: User
    let accessToken: String
    let refreshToken: String
}

struct Attachment: Codable, Equatable {
    let url: String
    let fileName: String
    let mimeType: String
    let size: Int
}

struct MessageSender: Codable, Equatable {
    let id: String
    let fullName: String
    let profilePicture: String

    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case fullName, profilePicture
    }
}

struct Message: Codable, Identifiable, Equatable {
    let id: String
    let chat: String
    let sender: MessageSender
    let type: String
    var text: String
    var attachment: Attachment?
    var systemEvent: String?
    let createdAt: String

    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case chat, sender, type, text, attachment, systemEvent, createdAt
    }
}

struct LastMessage: Codable, Equatable {
    let text: String
    let senderId: String
    let type: String
    let createdAt: String
}

struct ChatPeer: Codable, Equatable {
    let id: String
    let fullName: String
    let profilePicture: String
    let designation: String
    let status: String
}

struct ChatSummary: Codable, Identifiable, Equatable {
    let id: String
    let type: String
    var lastMessage: LastMessage?
    var unreadCount: Int
    var updatedAt: String
    var name: String?
    var avatar: String?
    var memberCount: Int?
    var peer: ChatPeer?

    var displayName: String { type == "group" ? (name ?? "Group") : (peer?.fullName ?? "Unknown") }
    var displayAvatar: String { type == "group" ? (avatar ?? "") : (peer?.profilePicture ?? "") }
}

struct GroupMember: Codable, Identifiable, Equatable {
    let id: String
    let role: String
    let fullName: String
    let profilePicture: String
    let designation: String
    let status: String
}

struct GroupInfo: Codable, Identifiable, Equatable {
    let id: String
    var type: String
    var name: String
    var avatar: String
    var createdBy: String?
    var createdAt: String?
    var members: [GroupMember]
}

struct NotificationActor: Codable, Equatable {
    let id: String
    let fullName: String
    let profilePicture: String

    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case fullName, profilePicture
    }
}

struct AppNotification: Codable, Identifiable, Equatable {
    let id: String
    let user: String
    let type: String
    let title: String
    let body: String
    let chat: String?
    let actor: NotificationActor?
    var read: Bool
    let createdAt: String

    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case user, type, title, body, chat, actor, read, createdAt
    }
}
