import SwiftUI

@main
struct CoChatApp: App {
    @StateObject private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(container)
                .environmentObject(container.tokenStore)
                .environmentObject(container.realtimeStore)
        }
    }
}
