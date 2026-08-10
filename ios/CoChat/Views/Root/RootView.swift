import SwiftUI

enum AuthRoute {
    case login, register
}

struct RootView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var tokenStore: TokenStore
    @State private var authRoute: AuthRoute = .login

    var body: some View {
        Group {
            if tokenStore.currentUser != nil {
                MainTabView()
                    .task { await container.realtimeStore.start() }
            } else {
                switch authRoute {
                case .login: LoginView(route: $authRoute)
                case .register: RegisterView(route: $authRoute)
                }
            }
        }
    }
}

/// Push destinations reachable from the Chats tab.
enum ChatsRoute: Hashable {
    case thread(chatId: String)
    case createGroup
    case groupInfo(groupId: String)
}

struct MainTabView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var realtimeStore: RealtimeStore
    @State private var selectedTab = 0
    @State private var chatsPath = NavigationPath()

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack(path: $chatsPath) {
                ChatListView(
                    onOpenChat: { chatsPath.append(ChatsRoute.thread(chatId: $0)) },
                    onNewChat: { selectedTab = 1 },
                    onNewGroup: { chatsPath.append(ChatsRoute.createGroup) }
                )
                .navigationDestination(for: ChatsRoute.self) { route in
                    switch route {
                    case .thread(let chatId):
                        ChatThreadView(chatId: chatId, onOpenGroupInfo: { chatsPath.append(ChatsRoute.groupInfo(groupId: $0)) })
                    case .createGroup:
                        CreateGroupView(onCreated: { groupId in
                            chatsPath.removeLast(chatsPath.count)
                            chatsPath.append(ChatsRoute.thread(chatId: groupId))
                        })
                    case .groupInfo(let groupId):
                        GroupInfoView(groupId: groupId, onLeft: { chatsPath.removeLast(chatsPath.count) })
                    }
                }
            }
            .tabItem { Label("Chats", systemImage: "bubble.left.and.bubble.right") }
            .badge(realtimeStore.totalUnreadChats)
            .tag(0)

            NavigationStack {
                TeamView(onOpenChat: { chatId in
                    selectedTab = 0
                    chatsPath.append(ChatsRoute.thread(chatId: chatId))
                })
            }
            .tabItem { Label("Team", systemImage: "person.2") }
            .tag(1)

            NavigationStack {
                NotificationsView(onOpenChat: { chatId in
                    selectedTab = 0
                    chatsPath.append(ChatsRoute.thread(chatId: chatId))
                })
            }
            .tabItem { Label("Alerts", systemImage: "bell") }
            .badge(realtimeStore.unreadNotificationCount)
            .tag(2)

            NavigationStack {
                SettingsView()
            }
            .tabItem { Label("Settings", systemImage: "gearshape") }
            .tag(3)
        }
        .tint(.indigo)
    }
}
