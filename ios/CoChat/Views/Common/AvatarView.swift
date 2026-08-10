import SwiftUI

struct AvatarView: View {
    let imageURL: String?
    let name: String
    var status: String? = nil
    var size: CGFloat = 44

    private var initials: String {
        let parts = name.trimmingCharacters(in: .whitespaces).split(separator: " ")
        if parts.isEmpty { return "?" }
        if parts.count == 1 { return String(parts[0].prefix(1)).uppercased() }
        return (parts.first!.prefix(1) + parts.last!.prefix(1)).uppercased()
    }

    private var statusColor: Color {
        switch status {
        case "online": return .green
        case "away": return .orange
        default: return .gray
        }
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Group {
                if let imageURL, let url = URL(string: imageURL), !imageURL.isEmpty {
                    AsyncImage(url: url) { phase in
                        if let image = phase.image {
                            image.resizable().aspectRatio(contentMode: .fill)
                        } else {
                            fallback
                        }
                    }
                } else {
                    fallback
                }
            }
            .frame(width: size, height: size)
            .clipShape(Circle())

            if let status {
                Circle()
                    .fill(statusColor)
                    .frame(width: size * 0.28, height: size * 0.28)
                    .overlay(Circle().stroke(Color(.systemBackground), lineWidth: 2))
                    .opacity(status.isEmpty ? 0 : 1)
            }
        }
    }

    private var fallback: some View {
        Circle()
            .fill(Color.indigo.opacity(0.2))
            .overlay(Text(initials).font(.system(size: size * 0.36, weight: .bold)).foregroundColor(.indigo))
    }
}
