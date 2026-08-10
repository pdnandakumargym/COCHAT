import SwiftUI
import UIKit
import UniformTypeIdentifiers

private let emojiSet = ["😀", "😂", "😍", "👍", "🙏", "🎉", "❤️", "😢", "😮", "🔥", "✅", "👏"]

struct ChatThreadView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var realtimeStore: RealtimeStore
    @EnvironmentObject private var tokenStore: TokenStore
    let chatId: String
    let onOpenGroupInfo: (String) -> Void

    @State private var chat: ChatSummary?
    @State private var messages: [Message] = []
    @State private var loading = true
    @State private var hasMore = true
    @State private var loadingOlder = false
    @State private var draftText = ""
    @State private var pendingFileURL: URL?
    @State private var showEmojiPicker = false
    @State private var showFileImporter = false
    @State private var typingTask: Task<Void, Never>?

    private var currentUserId: String { tokenStore.currentUser?.id ?? "" }
    private var isGroup: Bool { chat?.type == "group" }
    private var typists: Set<String> { realtimeStore.typing[chatId] ?? [] }
    private var peerStatus: String? {
        guard let peer = chat?.peer else { return nil }
        return realtimeStore.presence[peer.id] ?? peer.status
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 2) {
                        if hasMore && !messages.isEmpty {
                            Button(loadingOlder ? "Loading…" : "Load older messages") { Task { await loadOlder() } }
                                .font(.caption).padding(.vertical, 8)
                                .disabled(loadingOlder)
                        }
                        if messages.isEmpty && !loading {
                            Text("Say hello 👋 — no messages yet.").foregroundColor(.secondary).padding(.top, 60)
                        }
                        ForEach(messages) { message in
                            MessageBubbleView(
                                message: message,
                                isOwn: message.sender.id == currentUserId,
                                showSenderName: isGroup,
                                onOpenAttachment: openAttachment
                            )
                            .id(message.id)
                        }
                    }
                    .padding(.horizontal, 14).padding(.vertical, 10)
                }
                .onChange(of: messages.count) { _ in
                    if let last = messages.last { withAnimation { proxy.scrollTo(last.id, anchor: .bottom) } }
                }
            }

            if let pendingFileURL {
                HStack {
                    Text("📎 \(pendingFileURL.lastPathComponent)").font(.caption)
                    Spacer()
                    Button { self.pendingFileURL = nil } label: { Image(systemName: "xmark.circle.fill") }
                }
                .padding(.horizontal)
            }
            if showEmojiPicker {
                ScrollView(.horizontal) {
                    HStack {
                        ForEach(emojiSet, id: \.self) { emoji in
                            Text(emoji).font(.title2).onTapGesture {
                                draftText += emoji
                                showEmojiPicker = false
                            }
                        }
                    }.padding(.horizontal)
                }
            }
            HStack(spacing: 8) {
                Button { showFileImporter = true } label: { Image(systemName: "paperclip") }
                Button { showEmojiPicker.toggle() } label: { Image(systemName: "face.smiling") }
                TextField("Type a message…", text: $draftText, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: draftText) { _ in onTypingChanged() }
                Button(action: { Task { await send() } }) { Image(systemName: "paperplane.fill") }
            }
            .padding(10)
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Button {
                    if isGroup { onOpenGroupInfo(chatId) }
                } label: {
                    HStack(spacing: 8) {
                        if let chat {
                            AvatarView(imageURL: chat.displayAvatar, name: chat.displayName, status: isGroup ? nil : peerStatus, size: 32)
                            VStack(alignment: .leading, spacing: 0) {
                                Text(chat.displayName).font(.headline).foregroundColor(.primary)
                                Text(subtitle).font(.caption2).foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }
        }
        .fileImporter(isPresented: $showFileImporter, allowedContentTypes: [.item], allowsMultipleSelection: false) { result in
            if case .success(let urls) = result, let url = urls.first { pendingFileURL = url }
        }
        .task { await load() }
        .onChange(of: realtimeStore.lastIncomingMessage) { incoming in
            guard let incoming, incoming.chatId == chatId else { return }
            appendIfNew(incoming.message)
            if incoming.message.sender.id != currentUserId {
                Task { try? await container.chatsRepository.markRead(chatId: chatId) }
                realtimeStore.markChatReadLocally(chatId)
            }
        }
        .onChange(of: realtimeStore.lastGroupUpdate) { update in
            guard let update, update.chat.id == chatId else { return }
            chat = ChatSummary(
                id: update.chat.id, type: "group", lastMessage: chat?.lastMessage, unreadCount: chat?.unreadCount ?? 0,
                updatedAt: chat?.updatedAt ?? "", name: update.chat.name, avatar: update.chat.avatar,
                memberCount: update.chat.members.count, peer: nil
            )
        }
    }

    private var subtitle: String {
        if !typists.isEmpty { return typists.count == 1 ? "Typing…" : "\(typists.count) people typing…" }
        if isGroup { return "\(chat?.memberCount ?? 0) members" }
        switch peerStatus {
        case "online": return "Online"
        case "away": return "Away"
        default: return "Offline"
        }
    }

    private func load() async {
        loading = true
        chat = realtimeStore.chats.first { $0.id == chatId }
        if chat == nil, let list = try? await container.chatsRepository.list() {
            chat = list.first { $0.id == chatId }
        }
        messages = (try? await container.chatsRepository.messages(chatId: chatId)) ?? []
        loading = false
        try? await container.chatsRepository.markRead(chatId: chatId)
        realtimeStore.markChatReadLocally(chatId)
    }

    private func loadOlder() async {
        guard let first = messages.first, !loadingOlder else { return }
        loadingOlder = true
        let older = (try? await container.chatsRepository.messages(chatId: chatId, before: first.id)) ?? []
        if older.isEmpty { hasMore = false }
        messages = older + messages
        loadingOlder = false
    }

    private func appendIfNew(_ message: Message) {
        guard !messages.contains(where: { $0.id == message.id }) else { return }
        messages.append(message)
    }

    private func onTypingChanged() {
        realtimeStore.emitTypingStart(chatId: chatId)
        typingTask?.cancel()
        typingTask = Task {
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            guard !Task.isCancelled else { return }
            realtimeStore.emitTypingStop(chatId: chatId)
        }
    }

    private func send() async {
        let text = draftText.trimmingCharacters(in: .whitespacesAndNewlines)
        let fileURL = pendingFileURL
        guard !text.isEmpty || fileURL != nil else { return }
        draftText = ""
        pendingFileURL = nil
        typingTask?.cancel()
        realtimeStore.emitTypingStop(chatId: chatId)

        var fileData: Data?
        var mimeType: String?
        var fileName: String?
        if let fileURL {
            let accessed = fileURL.startAccessingSecurityScopedResource()
            defer { if accessed { fileURL.stopAccessingSecurityScopedResource() } }
            fileData = try? Data(contentsOf: fileURL)
            fileName = fileURL.lastPathComponent
            mimeType = UTType(filenameExtension: fileURL.pathExtension)?.preferredMIMEType ?? "application/octet-stream"
        }

        if let message = try? await container.chatsRepository.sendMessage(
            chatId: chatId, text: text.isEmpty ? nil : text, fileData: fileData, fileName: fileName, mimeType: mimeType
        ) {
            appendIfNew(message)
        }
    }

    private func openAttachment(_ urlString: String) {
        guard let url = URL(string: urlString) else { return }
        UIApplication.shared.open(url)
    }
}
