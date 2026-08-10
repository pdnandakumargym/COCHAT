import SwiftUI
import UserNotifications

struct SettingsView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var tokenStore: TokenStore
    @EnvironmentObject private var realtimeStore: RealtimeStore
    @AppStorage("cochat.pushNotificationsEnabled") private var notificationsEnabled = false
    @State private var permissionDenied = false

    var body: some View {
        Form {
            Section {
                if let user = tokenStore.currentUser {
                    NavigationLink {
                        ProfileView()
                    } label: {
                        HStack {
                            AvatarView(imageURL: user.profilePicture, name: user.fullName, status: realtimeStore.presence[user.id] ?? user.status, size: 44)
                            VStack(alignment: .leading) {
                                Text(user.fullName).fontWeight(.semibold)
                                Text(user.designation.isEmpty ? "View profile" : user.designation)
                                    .font(.caption).foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }

            Section("Notifications") {
                Toggle("Push notifications", isOn: $notificationsEnabled)
                    .onChange(of: notificationsEnabled) { enabled in
                        if enabled { requestPushPermission() }
                    }
                Text("Get notified of new messages while CoChat is in the background")
                    .font(.caption).foregroundColor(.secondary)
                if permissionDenied {
                    Text("Notifications are blocked in iOS Settings for CoChat.")
                        .font(.caption).foregroundColor(.orange)
                }
            }

            Section("Account") {
                if let user = tokenStore.currentUser {
                    LabeledContent("Signed in as", value: user.email ?? user.mobile ?? "")
                }
                Button("Log out", role: .destructive) { Task { await logout() } }
            }
        }
        .navigationTitle("Settings")
    }

    private func requestPushPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            DispatchQueue.main.async {
                permissionDenied = !granted
                notificationsEnabled = granted
            }
        }
    }

    private func logout() async {
        realtimeStore.stop()
        await container.authRepository.logout()
    }
}
