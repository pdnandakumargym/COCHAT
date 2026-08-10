import SwiftUI

struct FilePreviewView: View {
    let attachment: Attachment
    let type: String
    var onOpen: (String) -> Void = { _ in }

    var body: some View {
        switch type {
        case "image":
            if let url = URL(string: attachment.url) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    } else {
                        Color.gray.opacity(0.15)
                    }
                }
                .frame(maxWidth: 220, maxHeight: 220)
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .onTapGesture { onOpen(attachment.url) }
            }
        default:
            Button { onOpen(attachment.url) } label: {
                HStack(spacing: 8) {
                    Image(systemName: "doc.text")
                    VStack(alignment: .leading, spacing: 2) {
                        Text(attachment.fileName).font(.caption).fontWeight(.semibold).lineLimit(1)
                        Text(formatBytes(attachment.size)).font(.caption2).foregroundColor(.secondary)
                    }
                    Image(systemName: "arrow.down.circle")
                }
                .padding(8)
                .background(Color.black.opacity(0.05))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            .foregroundColor(.primary)
        }
    }

    private func formatBytes(_ bytes: Int) -> String {
        if bytes < 1024 { return "\(bytes) B" }
        if bytes < 1024 * 1024 { return String(format: "%.1f KB", Double(bytes) / 1024) }
        return String(format: "%.1f MB", Double(bytes) / (1024 * 1024))
    }
}
