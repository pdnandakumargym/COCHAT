import SwiftUI

struct CreateGroupView: View {
    @EnvironmentObject private var container: AppContainer
    let onCreated: (String) -> Void

    @State private var name = ""
    @State private var members: [User] = []
    @State private var selected: Set<String> = []
    @State private var saving = false
    @State private var error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let error {
                Text(error).font(.footnote).foregroundColor(.red)
                    .padding(10).background(Color.red.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 8))
                    .padding(.horizontal)
            }
            TextField("Group name", text: $name)
                .textFieldStyle(.roundedBorder)
                .padding(.horizontal)
            Text("Add members (\(selected.count) selected)")
                .font(.footnote).foregroundColor(.secondary)
                .padding(.horizontal)

            List(members) { member in
                Button {
                    if selected.contains(member.id) { selected.remove(member.id) } else { selected.insert(member.id) }
                } label: {
                    HStack {
                        Image(systemName: selected.contains(member.id) ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(.indigo)
                        AvatarView(imageURL: member.profilePicture, name: member.fullName, size: 36)
                        VStack(alignment: .leading) {
                            Text(member.fullName).foregroundColor(.primary)
                            Text(member.designation.isEmpty ? "Team member" : member.designation)
                                .font(.caption).foregroundColor(.secondary)
                        }
                    }
                }
            }
            .listStyle(.plain)

            Button(action: create) {
                HStack {
                    Spacer()
                    Text(saving ? "Creating…" : "Create group").fontWeight(.semibold)
                    Spacer()
                }
                .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
            .tint(.indigo)
            .disabled(saving)
            .padding(.horizontal)
        }
        .navigationTitle("New Group")
        .task {
            members = (try? await container.usersRepository.list()) ?? []
        }
    }

    private func create() {
        guard !name.isEmpty else { error = "Give your group a name."; return }
        guard !selected.isEmpty else { error = "Add at least one team member."; return }
        saving = true
        error = nil
        Task {
            do {
                let group = try await container.groupsRepository.create(name: name, memberIds: Array(selected))
                onCreated(group.id)
            } catch {
                self.error = "Could not create group."
            }
            saving = false
        }
    }
}
