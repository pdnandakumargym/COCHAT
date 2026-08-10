import SwiftUI

struct GroupInfoView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var realtimeStore: RealtimeStore
    @EnvironmentObject private var tokenStore: TokenStore
    let groupId: String
    let onLeft: () -> Void

    @State private var group: GroupInfo?
    @State private var loading = true
    @State private var showAddMembers = false
    @State private var candidates: [User] = []
    @State private var error: String?

    private var currentUserId: String { tokenStore.currentUser?.id ?? "" }
    private var isAdmin: Bool { group?.members.contains { $0.id == currentUserId && $0.role == "admin" } == true }

    var body: some View {
        Group {
            if loading || group == nil {
                ProgressView()
            } else if let group {
                List {
                    Section {
                        VStack(spacing: 8) {
                            AvatarView(imageURL: group.avatar, name: group.name, size: 84)
                            Text(group.name).font(.title3).fontWeight(.semibold)
                            Text("\(group.members.count) members").font(.caption).foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .listRowSeparator(.hidden)
                    }

                    Section {
                        ForEach(group.members) { member in
                            HStack {
                                AvatarView(imageURL: member.profilePicture, name: member.fullName, status: realtimeStore.presence[member.id] ?? member.status, size: 40)
                                VStack(alignment: .leading) {
                                    Text(member.fullName + (member.id == currentUserId ? " (You)" : ""))
                                    Text(member.designation.isEmpty ? "Team member" : member.designation)
                                        .font(.caption).foregroundColor(.secondary)
                                }
                                Spacer()
                                if member.role == "admin" {
                                    Text("Admin").font(.caption2).fontWeight(.bold)
                                        .padding(.horizontal, 8).padding(.vertical, 2)
                                        .background(Color.indigo.opacity(0.15)).foregroundColor(.indigo)
                                        .clipShape(Capsule())
                                } else if isAdmin {
                                    Button("Remove") { Task { await removeMember(member.id) } }
                                        .font(.caption).foregroundColor(.red)
                                }
                            }
                        }
                    } header: {
                        HStack {
                            Text("Members")
                            Spacer()
                            if isAdmin {
                                Button("+ Add members") { openAddMembers() }
                            }
                        }
                    }

                    Section {
                        Button("Leave group", role: .destructive) { Task { await leave() } }
                    }
                }
            }
        }
        .navigationTitle("Group Info")
        .navigationBarTitleDisplayMode(.inline)
        .task { group = try? await container.groupsRepository.get(id: groupId); loading = false }
        .onChange(of: realtimeStore.lastGroupUpdate) { update in
            guard let update, update.chat.id == groupId else { return }
            group = update.chat
        }
        .sheet(isPresented: $showAddMembers) {
            NavigationStack {
                List(candidates) { candidate in
                    Button { Task { await addMember(candidate.id) } } label: {
                        HStack {
                            AvatarView(imageURL: candidate.profilePicture, name: candidate.fullName, size: 32)
                            Text(candidate.fullName).foregroundColor(.primary)
                        }
                    }
                }
                .navigationTitle("Add members")
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("Done") { showAddMembers = false }
                    }
                }
            }
        }
    }

    private func openAddMembers() {
        showAddMembers = true
        Task {
            let all = (try? await container.usersRepository.list()) ?? []
            let existing = Set(group?.members.map(\.id) ?? [])
            candidates = all.filter { !existing.contains($0.id) }
        }
    }

    private func addMember(_ userId: String) async {
        if let updated = try? await container.groupsRepository.addMembers(id: groupId, memberIds: [userId]) {
            group = updated
            candidates.removeAll { $0.id == userId }
        }
    }

    private func removeMember(_ userId: String) async {
        do {
            try await container.groupsRepository.removeMember(id: groupId, userId: userId)
            group?.members.removeAll { $0.id == userId }
        } catch {
            self.error = "Could not remove member."
        }
    }

    private func leave() async {
        try? await container.groupsRepository.leave(id: groupId)
        onLeft()
    }
}
