import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var tokenStore: TokenStore
    @EnvironmentObject private var realtimeStore: RealtimeStore
    @State private var showEdit = false

    var body: some View {
        if let user = tokenStore.currentUser {
            ScrollView {
                VStack(spacing: 16) {
                    AvatarView(imageURL: user.profilePicture, name: user.fullName, status: realtimeStore.presence[user.id] ?? user.status, size: 96)
                    Text(user.fullName).font(.title2).fontWeight(.semibold)
                    Text(user.designation.isEmpty ? "Team member" : user.designation).foregroundColor(.secondary)

                    VStack(spacing: 0) {
                        DetailRow(label: "Email", value: user.email ?? "—")
                        Divider()
                        DetailRow(label: "Mobile", value: user.mobile ?? "—")
                        Divider()
                        DetailRow(label: "Designation", value: user.designation.isEmpty ? "—" : user.designation)
                    }
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                    Button("Edit profile") { showEdit = true }
                        .buttonStyle(.borderedProminent)
                        .tint(.indigo)
                        .frame(maxWidth: .infinity)
                }
                .padding(24)
            }
            .navigationTitle("Profile")
            .sheet(isPresented: $showEdit) {
                NavigationStack { ProfileEditView() }
            }
        }
    }
}

private struct DetailRow: View {
    let label: String
    let value: String
    var body: some View {
        HStack {
            Text(label).foregroundColor(.secondary)
            Spacer()
            Text(value).fontWeight(.medium)
        }
        .padding()
    }
}
