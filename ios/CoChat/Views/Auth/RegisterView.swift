import SwiftUI

struct RegisterView: View {
    @EnvironmentObject private var container: AppContainer
    @State private var fullName = ""
    @State private var useEmail = true
    @State private var email = ""
    @State private var mobile = ""
    @State private var designation = ""
    @State private var password = ""
    @State private var loading = false
    @State private var error: String?
    @Binding var route: AuthRoute

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Spacer(minLength: 24)
                Text("CoChat").font(.largeTitle).fontWeight(.bold).foregroundColor(.indigo)
                Text("Create your team account").foregroundColor(.secondary)

                if let error {
                    Text(error).font(.footnote).foregroundColor(.red)
                        .padding(10).background(Color.red.opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 8))
                }

                TextField("Full name", text: $fullName).textFieldStyle(.roundedBorder)

                Picker("Identifier", selection: $useEmail) {
                    Text("Email").tag(true)
                    Text("Mobile number").tag(false)
                }
                .pickerStyle(.segmented)

                if useEmail {
                    TextField("Email address", text: $email)
                        .textFieldStyle(.roundedBorder)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                } else {
                    TextField("Mobile number", text: $mobile)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.phonePad)
                }

                TextField("Designation (optional)", text: $designation).textFieldStyle(.roundedBorder)
                SecureField("Password", text: $password).textFieldStyle(.roundedBorder)

                Button(action: submit) {
                    HStack {
                        Spacer()
                        Text(loading ? "Creating account…" : "Create account").fontWeight(.semibold)
                        Spacer()
                    }
                    .padding(.vertical, 12)
                }
                .buttonStyle(.borderedProminent)
                .tint(.indigo)
                .disabled(loading)

                HStack {
                    Spacer()
                    Text("Already have an account?").foregroundColor(.secondary)
                    Button("Sign in") { route = .login }
                    Spacer()
                }
                .font(.footnote)
            }
            .padding(24)
        }
    }

    private func submit() {
        guard !fullName.isEmpty, password.count >= 6 else {
            error = "Full name is required and password must be at least 6 characters."
            return
        }
        if useEmail && email.isEmpty { error = "Enter your email address."; return }
        if !useEmail && mobile.isEmpty { error = "Enter your mobile number."; return }

        loading = true
        error = nil
        Task {
            do {
                try await container.authRepository.register(
                    fullName: fullName,
                    email: useEmail ? email : nil,
                    mobile: useEmail ? nil : mobile,
                    password: password,
                    designation: designation
                )
            } catch {
                self.error = "Registration failed."
            }
            loading = false
        }
    }
}
