import SwiftUI

struct NotificationsView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var realtimeStore: RealtimeStore
    let onOpenChat: (String) -> Void

    var body: some View {
        Group {
            if realtimeStore.notifications.isEmpty {
                Text("You're all caught up.").foregroundColor(.secondary)
            } else {
                List(realtimeStore.notifications) { notification in
                    Button { Task { await open(notification) } } label: {
                        HStack(alignment: .top, spacing: 12) {
                            if let actor = notification.actor {
                                AvatarView(imageURL: actor.profilePicture, name: actor.fullName, size: 40)
                            } else {
                                Text(icon(for: notification.type)).font(.title2)
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text(notification.title).fontWeight(.semibold)
                                Text(notification.body).font(.subheadline).foregroundColor(.secondary)
                                Text(notification.createdAt).font(.caption2).foregroundColor(Color(.tertiaryLabel))
                            }
                            Spacer()
                            if !notification.read {
                                Circle().fill(Color.indigo).frame(width: 8, height: 8)
                            }
                        }
                    }
                    .foregroundColor(.primary)
                    .listRowBackground(notification.read ? Color.clear : Color.indigo.opacity(0.06))
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Notifications")
        .toolbar {
            if !realtimeStore.notifications.isEmpty {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Mark all read") { Task { await markAllRead() } }
                }
            }
        }
    }

    private func icon(for type: String) -> String {
        switch type {
        case "private_message": return "💬"
        case "group_message": return "👥"
        case "group_created": return "🎉"
        case "member_added": return "➕"
        case "member_removed": return "➖"
        default: return "🔔"
        }
    }

    private func open(_ notification: AppNotification) async {
        if !notification.read {
            try? await container.notificationsRepository.markRead(id: notification.id)
            if let idx = realtimeStore.notifications.firstIndex(where: { $0.id == notification.id }) {
                realtimeStore.notifications[idx].read = true
            }
        }
        if let chatId = notification.chat { onOpenChat(chatId) }
    }

    private func markAllRead() async {
        try? await container.notificationsRepository.markAllRead()
        realtimeStore.notifications = realtimeStore.notifications.map { var n = $0; n.read = true; return n }
    }
}
