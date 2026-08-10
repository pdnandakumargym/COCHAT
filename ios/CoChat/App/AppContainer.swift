import Foundation

@MainActor
final class AppContainer: ObservableObject {
    let tokenStore: TokenStore
    let api: APIClient
    let authRepository: AuthRepository
    let usersRepository: UsersRepository
    let chatsRepository: ChatsRepository
    let groupsRepository: GroupsRepository
    let notificationsRepository: NotificationsRepository
    let realtimeStore: RealtimeStore

    init() {
        let tokenStore = TokenStore()
        let api = APIClient(tokenStore: tokenStore)
        let socket = AppSocketManager()

        self.tokenStore = tokenStore
        self.api = api
        self.authRepository = AuthRepository(api: api, tokenStore: tokenStore)
        self.usersRepository = UsersRepository(api: api, tokenStore: tokenStore)
        self.chatsRepository = ChatsRepository(api: api)
        self.groupsRepository = GroupsRepository(api: api)
        self.notificationsRepository = NotificationsRepository(api: api)
        self.realtimeStore = RealtimeStore(socket: socket, chatsRepo: chatsRepository, notificationsRepo: notificationsRepository, tokenStore: tokenStore)
    }
}
