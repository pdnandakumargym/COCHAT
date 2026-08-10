import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var container: AppContainer
    @State private var identifier = ""
    @State private var password = ""
    @State private var loading = false
    @State private var error: String?
    @Binding var route: AuthRoute

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Spacer(minLength: 40)
                Text("CoChat").font(.largeTitle).fontWeight(.bold).foregroundColor(.indigo)
                Text("Sign in to your team workspace").foregroundColor(.secondary)

                if let error {
                    Text(error).font(.footnote).foregroundColor(.red)
                        .padding(10).background(Color.red.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 8))
                }

                TextField("Email or mobile number", text: $identifier)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()

                SecureField("Password", text: $password)
                    .textFieldStyle(.roundedBorder)

                Button(action: submit) {
                    HStack {
                        Spacer()
                        Text(loading ? "Signing in…" : "Sign in").fontWeight(.semibold)
                        Spacer()
                    }
                    .padding(.vertical, 12)
                }
                .buttonStyle(.borderedProminent)
                .tint(.indigo)
                .disabled(loading)

                HStack {
                    Spacer()
                    Text("Don't have an account?").foregroundColor(.secondary)
                    Button("Create one") { route = .register }
                    Spacer()
                }
                .font(.footnote)
            }
            .padding(24)
        }
    }

    private func submit() {
        guard !identifier.isEmpty, !password.isEmpty else {
            error = "Enter your email/mobile and password."
            return
        }
        loading = true
        error = nil
        Task {
            do {
                try await container.authRepository.login(identifier: identifier, password: password)
            } catch {
                self.error = "Login failed. Check your credentials."
            }
            loading = false
        }
    }
}
