import SwiftUI

struct TeamView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var realtimeStore: RealtimeStore
    let onOpenChat: (String) -> Void

    @State private var query = ""
    @State private var members: [User] = []
    @State private var loading = true
    @State private var searchTask: Task<Void, Never>?

    var body: some View {
        Group {
            if loading {
                ProgressView()
            } else if members.isEmpty {
                Text("No team members match your search.").foregroundColor(.secondary)
            } else {
                List(members) { member in
                    HStack {
                        AvatarView(imageURL: member.profilePicture, name: member.fullName, status: realtimeStore.presence[member.id] ?? member.status, size: 48)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(member.fullName)
                            Text(member.designation.isEmpty ? "Team member" : member.designation)
                                .font(.caption).foregroundColor(.indigo)
                            Text(member.email ?? member.mobile ?? "").font(.caption2).foregroundColor(.secondary)
                        }
                        Spacer()
                        Button("Message") { Task { await startChat(with: member.id) } }
                            .font(.footnote)
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Team Members")
        .searchable(text: $query, prompt: "Search by name, email, mobile, or designation…")
        .onChange(of: query) { newValue in
            searchTask?.cancel()
            searchTask = Task {
                try? await Task.sleep(nanoseconds: 250_000_000)
                guard !Task.isCancelled else { return }
                await search(newValue)
            }
        }
        .task { await search("") }
    }

    private func search(_ text: String) async {
        loading = true
        members = (try? await container.usersRepository.list(query: text)) ?? []
        loading = false
    }

    private func startChat(with userId: String) async {
        guard let chat = try? await container.chatsRepository.openPrivate(userId: userId) else { return }
        realtimeStore.upsertChat(chat)
        onOpenChat(chat.id)
    }
}
