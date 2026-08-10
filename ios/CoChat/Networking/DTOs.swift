import Foundation

struct UserEnvelope: Codable { let user: User }
struct UsersEnvelope: Codable { let users: [User] }
struct ChatsEnvelope: Codable { let chats: [ChatSummary] }
struct ChatEnvelope: Codable { let chat: ChatSummary }
struct MessagesEnvelope: Codable { let messages: [Message] }
struct MessageEnvelope: Codable { let message: Message }
struct GroupEnvelope: Codable { let group: GroupInfo }
struct NotificationsEnvelope: Codable { let notifications: [AppNotification] }

struct RegisterRequest: Codable {
    let fullName: String
    let email: String?
    let mobile: String?
    let password: String
    let designation: String?
}

struct LoginRequest: Codable { let identifier: String; let password: String }
struct RefreshRequest: Codable { let refreshToken: String }
struct UpdateProfileRequest: Codable { let fullName: String?; let designation: String? }
struct OpenPrivateChatRequest: Codable { let userId: String }
struct CreateGroupRequest: Codable { let name: String; let memberIds: [String] }
struct AddMembersRequest: Codable { let memberIds: [String] }
