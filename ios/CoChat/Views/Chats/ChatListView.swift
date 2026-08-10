import SwiftUI

struct ChatListView: View {
    @EnvironmentObject private var realtimeStore: RealtimeStore
    let onOpenChat: (String) -> Void
    let onNewChat: () -> Void
    let onNewGroup: () -> Void

    var body: some View {
        Group {
            if realtimeStore.chats.isEmpty {
                VStack(spacing: 8) {
                    Text("No conversations yet.").foregroundColor(.secondary)
                    Button("Find a team member to start chatting", action: onNewChat)
                        .font(.footnote)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(realtimeStore.chats) { chat in
                    Button { onOpenChat(chat.id) } label: {
                        ChatRow(chat: chat, livePeerStatus: chat.peer.flatMap { realtimeStore.presence[$0.id] })
                    }
                    .foregroundColor(.primary)
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Chats")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                Button(action: onNewChat) { Image(systemName: "person.badge.plus") }
                Button(action: onNewGroup) { Image(systemName: "person.3") }
            }
        }
    }
}

private struct ChatRow: View {
    let chat: ChatSummary
    let livePeerStatus: String?

    private var previewText: String {
        guard let lastMessage = chat.lastMessage else { return "No messages yet" }
        return lastMessage.type == "text" ? lastMessage.text : "Sent a \(lastMessage.type)"
    }

    var body: some View {
        HStack(spacing: 12) {
            AvatarView(imageURL: chat.displayAvatar, name: chat.displayName, status: chat.type == "private" ? (livePeerStatus ?? chat.peer?.status) : nil, size: 48)
            VStack(alignment: .leading, spacing: 3) {
                Text(chat.displayName).fontWeight(.semibold)
                HStack {
                    Text(previewText).font(.subheadline).foregroundColor(.secondary).lineLimit(1)
                    Spacer()
                    if chat.unreadCount > 0 {
                        Text("\(chat.unreadCount)")
                            .font(.caption2).fontWeight(.bold).foregroundColor(.white)
                            .padding(.horizontal, 7).padding(.vertical, 2)
                            .background(Color.indigo).clipShape(Capsule())
                    }
                }
            }
        }
        .padding(.vertical, 4)
    }
}
