import SwiftUI
import PhotosUI

struct ProfileEditView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var tokenStore: TokenStore
    @Environment(\.dismiss) private var dismiss

    @State private var fullName = ""
    @State private var designation = ""
    @State private var saving = false
    @State private var uploadingAvatar = false
    @State private var success: String?
    @State private var error: String?
    @State private var avatarItem: PhotosPickerItem?

    var body: some View {
        Form {
            if let error {
                Text(error).foregroundColor(.red)
            }
            if let success {
                Text(success).foregroundColor(.green)
            }

            Section {
                HStack {
                    Spacer()
                    ZStack(alignment: .bottomTrailing) {
                        AvatarView(imageURL: tokenStore.currentUser?.profilePicture, name: fullName, size: 84)
                        PhotosPicker(selection: $avatarItem, matching: .images) {
                            Image(systemName: "pencil.circle.fill")
                                .font(.title2)
                                .foregroundColor(.indigo)
                                .background(Circle().fill(.white))
                        }
                    }
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }

            Section {
                TextField("Full name", text: $fullName)
                TextField("Designation", text: $designation)
            }
            Section {
                LabeledContent("Email", value: tokenStore.currentUser?.email ?? "—")
                LabeledContent("Mobile", value: tokenStore.currentUser?.mobile ?? "—")
            }

            Button(action: save) {
                HStack {
                    Spacer()
                    Text(saving ? "Saving…" : "Save changes")
                    Spacer()
                }
            }
            .disabled(saving)
        }
        .navigationTitle("Edit profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Close") { dismiss() }
            }
        }
        .onAppear {
            fullName = tokenStore.currentUser?.fullName ?? ""
            designation = tokenStore.currentUser?.designation ?? ""
        }
        .onChange(of: avatarItem) { newItem in
            Task { await uploadAvatar(newItem) }
        }
    }

    private func save() {
        guard !fullName.isEmpty else { error = "Full name is required."; return }
        saving = true
        error = nil
        success = nil
        Task {
            do {
                _ = try await container.usersRepository.updateProfile(fullName: fullName, designation: designation)
                success = "Profile updated."
            } catch {
                self.error = "Could not update profile."
            }
            saving = false
        }
    }

    private func uploadAvatar(_ item: PhotosPickerItem?) async {
        guard let item, let data = try? await item.loadTransferable(type: Data.self) else { return }
        uploadingAvatar = true
        _ = try? await container.usersRepository.uploadAvatar(data: data, fileName: "avatar.jpg", mimeType: "image/jpeg")
        uploadingAvatar = false
    }
}
