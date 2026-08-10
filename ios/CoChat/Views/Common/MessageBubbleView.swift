import SwiftUI

struct MessageBubbleView: View {
    let message: Message
    let isOwn: Bool
    var showSenderName: Bool = false
    var onOpenAttachment: (String) -> Void = { _ in }

    private var time: String {
        guard let date = ISO8601DateFormatter().date(from: message.createdAt) else { return "" }
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    var body: some View {
        if let systemEvent = message.systemEvent, !systemEvent.isEmpty {
            Text(message.text)
                .font(.caption)
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
        } else {
            HStack {
                if isOwn { Spacer(minLength: 40) }
                VStack(alignment: isOwn ? .trailing : .leading, spacing: 3) {
                    if showSenderName && !isOwn {
                        Text(message.sender.fullName)
                            .font(.caption2).fontWeight(.semibold).foregroundColor(.secondary)
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        if let attachment = message.attachment {
                            FilePreviewView(attachment: attachment, type: message.type, onOpen: onOpenAttachment)
                        }
                        if !message.text.isEmpty {
                            Text(message.text).foregroundColor(isOwn ? .white : .primary)
                        }
                        Text(time)
                            .font(.caption2)
                            .foregroundColor(isOwn ? .white.opacity(0.7) : .secondary)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    }
                    .padding(10)
                    .background(isOwn ? Color.indigo : Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                if !isOwn { Spacer(minLength: 40) }
            }
        }
    }
}
