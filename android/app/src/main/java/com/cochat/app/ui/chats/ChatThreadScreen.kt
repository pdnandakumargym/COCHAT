package com.cochat.app.ui.chats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cochat.app.data.model.ChatSummary
import com.cochat.app.data.model.Message
import com.cochat.app.data.repository.ChatsRepository
import com.cochat.app.data.repository.RealtimeStore
import com.cochat.app.ui.common.Avatar
import com.cochat.app.ui.common.LoadingState
import com.cochat.app.ui.common.MessageBubble
import com.cochat.app.util.mimeTypeOf
import com.cochat.app.util.uriToCacheFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EMOJIS = listOf("😀", "😂", "😍", "👍", "🙏", "🎉", "❤️", "😢", "😮", "🔥", "✅", "👏")

@Composable
fun ChatThreadScreen(
    chatId: String,
    chatsRepository: ChatsRepository,
    realtimeStore: RealtimeStore,
    currentUserId: String,
    onBack: () -> Unit,
    onOpenGroupInfo: (String) -> Unit,
    onOpenAttachment: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storeChats by realtimeStore.chats.collectAsState()
    val presence by realtimeStore.presence.collectAsState()
    val typing by realtimeStore.typing.collectAsState()
    val lastIncoming by realtimeStore.lastIncomingMessage.collectAsState()

    var chatSummary by remember(chatId) { mutableStateOf<ChatSummary?>(null) }
    var messages by remember(chatId) { mutableStateOf<List<Message>>(emptyList()) }
    var loading by remember(chatId) { mutableStateOf(true) }
    var draftText by remember(chatId) { mutableStateOf("") }
    var pendingFileUri by remember(chatId) { mutableStateOf<Uri?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingFileUri = uri
    }

    LaunchedEffect(chatId) {
        loading = true
        chatSummary = storeChats.find { it.id == chatId }
        if (chatSummary == null) {
            val list = chatsRepository.list()
            realtimeStore.chats.value = list
            chatSummary = list.find { it.id == chatId }
        }
        messages = chatsRepository.messages(chatId)
        loading = false
        chatsRepository.markRead(chatId)
        realtimeStore.markChatReadLocally(chatId)
    }

    LaunchedEffect(chatId) { snapshotFlow { storeChats }.collect { list -> list.find { it.id == chatId }?.let { chatSummary = it } } }

    LaunchedEffect(lastIncoming) {
        val (incomingChatId, message) = lastIncoming ?: return@LaunchedEffect
        if (incomingChatId != chatId) return@LaunchedEffect
        if (messages.none { it.id == message.id }) messages = messages + message
        if (message.sender.id != currentUserId) {
            chatsRepository.markRead(chatId)
            realtimeStore.markChatReadLocally(chatId)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // debounced typing:stop — restarts on every keystroke, fires once input goes idle
    LaunchedEffect(draftText) {
        if (draftText.isNotEmpty()) {
            realtimeStore.emitTypingStart(chatId)
            delay(1500)
            realtimeStore.emitTypingStop(chatId)
        }
    }

    fun send() {
        val text = draftText.trim()
        val uri = pendingFileUri
        if (text.isBlank() && uri == null) return
        draftText = ""
        pendingFileUri = null
        realtimeStore.emitTypingStop(chatId)
        scope.launch {
            val file = uri?.let { uriToCacheFile(context, it) }
            val mime = uri?.let { mimeTypeOf(context, it) }
            val message = chatsRepository.sendMessage(chatId, text.ifBlank { null }, file, mime)
            if (messages.none { it.id == message.id }) messages = messages + message
        }
    }

    val chat = chatSummary
    val isGroup = chat?.type == "group"
    val typists = typing[chatId] ?: emptySet()
    val peerStatus = chat?.peer?.let { presence[it.id] ?: it.status }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            },
            title = {
                if (chat != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(enabled = isGroup) { onOpenGroupInfo(chatId) },
                    ) {
                        Avatar(chat.displayAvatar, chat.displayName, if (isGroup) null else peerStatus, size = 36)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(chat.displayName, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            val subtitle = when {
                                typists.isNotEmpty() -> if (typists.size == 1) "Typing…" else "${typists.size} people typing…"
                                isGroup -> "${chat.memberCount ?: 0} members"
                                peerStatus == "online" -> "Online"
                                peerStatus == "away" -> "Away"
                                else -> "Offline"
                            }
                            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            },
        )

        Box(Modifier.weight(1f)) {
            when {
                loading -> LoadingState("Loading messages…")
                messages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Say hello 👋 — no messages yet.", color = Color.Gray)
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        if (message.systemEvent != null) {
                            Text(
                                message.text,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        } else {
                            MessageBubble(
                                text = message.text,
                                isOwn = message.sender.id == currentUserId,
                                time = message.createdAt.take(16).substringAfter('T'),
                                senderName = if (isGroup) message.sender.fullName else null,
                                attachment = message.attachment,
                                attachmentType = message.type,
                                onOpenAttachment = onOpenAttachment,
                            )
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
            pendingFileUri?.let {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Text("📎 attachment ready", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { pendingFileUri = null }) { Icon(Icons.Default.Close, contentDescription = "Remove") }
                }
            }
            if (showEmojiPicker) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    EMOJIS.forEach { emoji ->
                        Text(
                            emoji,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(4.dp).clickable {
                                draftText += emoji
                                showEmojiPicker = false
                            },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { filePicker.launch("*/*") }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                }
                IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                    Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji")
                }
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message…") },
                    shape = MaterialTheme.shapes.extraLarge,
                )
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { send() }) { Icon(Icons.Default.Send, contentDescription = "Send") }
            }
        }
    }
}
